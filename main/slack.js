"use strict";

const { verifyRequestSignature } = require("@slack/events-api");

function verify(req) {
    if (req.method !== "POST") {
        throw new Error("Only POST requests are accepted.");
    }
    const signature = {
        signingSecret: process.env.SLACK_SIGNING_SECRET,
        requestSignature: req.headers["x-slack-signature"],
        requestTimestamp: req.headers["x-slack-request-timestamp"],
        body: req.rawBody
    };
    verifyRequestSignature(signature);
}

module.exports = {
    verify
};