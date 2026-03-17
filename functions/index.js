/**
 * Email Verification with 5-digit code for EverythingBIM
 */

const { onCall } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const logger = require("firebase-functions/logger");

// Initialize Firebase Admin
admin.initializeApp();

// Set global options
setGlobalOptions({ maxInstances: 10 });

// Configure email transporter (UPDATE WITH YOUR EMAIL)
// For Gmail, you'll need to create an "App Password"
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: 'alexpbridgeman@gmail.com', // Replace with your email
        pass: 'ixmeynvdsnoxuead'     // Replace with your app password
    }
});

// Function to send verification code
exports.sendVerificationCode = onCall(async (request) => {
    const data = request.data;
    const email = data.email;
    const code = data.code;

    logger.info(`Sending verification code to ${email}`);

    // Store code in Firestore with expiration
    await admin.firestore().collection('verificationCodes').doc(email.replace(/\./g, ','))
        .set({
            code: code,
            email: email,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            expiresAt: new Date(Date.now() + 10 * 60 * 1000), // 10 minutes
            used: false
        });

    // Email content
    const mailOptions = {
        from: '"EverythingBIM" <YOUR-EMAIL@gmail.com>', // Replace with your email
        to: email,
        subject: 'Your Verification Code for EverythingBIM',
        html: `
            <div style="font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #333;">Email Verification</h2>
                <p style="font-size: 16px;">Thank you for registering with EverythingBIM!</p>
                <p style="font-size: 16px;">Your verification code is:</p>
                <div style="background: #f5f5f5; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0;">
                    <h1 style="font-size: 48px; letter-spacing: 8px; color: #2c3e50; margin: 0;">${code}</h1>
                </div>
                <p style="font-size: 14px; color: #666;">Enter this code in the app to verify your email address.</p>
                <p style="font-size: 14px; color: #e74c3c;">This code will expire in 10 minutes.</p>
                <p style="font-size: 12px; color: #999; margin-top: 30px;">If you didn't request this, please ignore this email.</p>
            </div>
        `
    };

    try {
        await transporter.sendMail(mailOptions);
        logger.info(`Code sent successfully to ${email}`);
        return { success: true, message: "Verification code sent" };
    } catch (error) {
        logger.error("Error sending email:", error);
        throw new Error(`Failed to send email: ${error.message}`);
    }
});

// Function to verify code
exports.verifyCode = onCall(async (request) => {
    const data = request.data;
    const email = data.email;
    const enteredCode = data.code;

    logger.info(`Verifying code for ${email}`);

    const docRef = admin.firestore().collection('verificationCodes')
        .doc(email.replace(/\./g, ','));

    const doc = await docRef.get();

    if (!doc.exists) {
        throw new Error('No verification code found. Please request a new code.');
    }

    const codeData = doc.data();
    const now = new Date();
    const expiresAt = codeData.expiresAt.toDate();

    // Check if expired
    if (expiresAt < now) {
        await docRef.delete();
        throw new Error('Code expired. Please request a new code.');
    }

    // Check if already used
    if (codeData.used) {
        throw new Error('Code already used. Please request a new code.');
    }

    // Verify code
    if (codeData.code === enteredCode) {
        await docRef.update({ used: true });
        logger.info(`Code verified successfully for ${email}`);
        return { success: true, message: "Code verified successfully" };
    } else {
        logger.warn(`Invalid code attempt for ${email}`);
        throw new Error('Invalid verification code');
    }
});

// Optional: Function to resend code
exports.resendVerificationCode = onCall(async (request) => {
    const data = request.data;
    const email = data.email;

    logger.info(`Resending verification code to ${email}`);

    // Delete old code
    const docRef = admin.firestore().collection('verificationCodes')
        .doc(email.replace(/\./g, ','));
    await docRef.delete();

    // Generate new code
    const newCode = String(Math.floor(10000 + Math.random() * 90000));

    // Call send function
    const sendFunction = exports.sendVerificationCode;
    return sendFunction.run({ data: { email, code: newCode } });
});
