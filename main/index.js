"use strict";

const functions = require("@google-cloud/functions-framework");
const { PubSub } = require("@google-cloud/pubsub");
const pubsub = new PubSub();

functions.http("useTool", (req, res) => {
  const topicName = "slack-tools.ci";
  const messagePayload = {
    message: "yoyoyo"
  };
  publishMessage(topicName, messagePayload)
    .then((result) => {
      console.log(result);
      console.log("成功");
    })
    .catch((error) => {
      console.log(error);
      console.log("失敗");
    });
  res.status(200).send("謝謝你 9527");
});

function publishMessage(topicName, messagePayload) {
  try {
    const topic = pubsub.topic(topicName);
    const messagePayloadBuffer = Buffer.from(JSON.stringify(messagePayload), "utf8");
    const messageId = topic.publishMessage({ data: messagePayloadBuffer });
    return Promise.resolve(messageId);
  } catch (error) {
    return Promise.reject(error);
  }
}