const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

exports.sendNotificationOnNewAppointment = functions.firestore
    .document("appointments/{appointmentId}")
    .onCreate(async (snap, context) => {
      try {
        const appointmentData = snap.data();
        if (!appointmentData) return null;

        const userId = appointmentData.ownerId;
        if (!userId) return null;

        const userDoc = await db.collection("users").doc(userId).get();
        if (!userDoc.exists) return null;

        const userData = userDoc.data();
        const fcmToken = userData && userData.fcmToken;
        if (!fcmToken) return null;

        const payload = {
          notification: {
            title: "Новая запись",
            body: "У вас новая запись на приём",
          },
          token: fcmToken,
        };

        await admin.messaging().send(payload);
        console.log("Notification sent to user:", userId);
        return null;
      } catch (error) {
        console.error("Error sending notification:", error);
        return null;
      }
    });
