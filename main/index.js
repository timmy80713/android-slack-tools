"use strict";

const axios = require("axios");
const functions = require("@google-cloud/functions-framework");
const { PubSub } = require("@google-cloud/pubsub");
const url = require("url");
const slack = require("./slack");

const pubsub = new PubSub();

functions.http("useTool", (req, res) => {
  console.log("The service has started.");
  console.log("Request from ==> ", req.body.team_domain, req.body.channel_name, req.body.user_name)
  console.log("Command ==> ", req.body.command)
  console.log("Text ==> ", req.body.text)

  try {
    slack.verify(req);
  } catch (error) {
    console.log("Slack verify failed.", error.message);
    res.status(200).send(error.message);
    return;
  }

  const teamId = req.body.team_id;
  if (teamId !== "T03180XEC") {
    const errorMessage = "You cannot use this slash command in this Slack team.";
    console.log(errorMessage);
    res.status(200).send(errorMessage);
    return;
  }

  const channelId = req.body.channel_id;
  const allowChannelIds = ["D025JAVQRRT", "C053SNHM2NM"];
  if (!allowChannelIds.includes(channelId)) {
    const errorMessage = "You cannot use this slash command in this channel.";
    console.log(errorMessage);
    res.status(200).send(errorMessage);
    return;
  }

  const userId = req.body.user_id;
  const userName = req.body.user_name;
  const command = req.body.command;
  const text = req.body.text;
  const responseUrl = req.body.response_url;
  const fullCommand = `${command} ${text}`;

  const [matchGroup, urlPath] = checkUrlPath(req.url);
  if (matchGroup == null) {
    console.log("Invalid URL path.");
    res.status(200).send("Not found.");
    return;
  }

  const slackPayload = {
    response_type: "ephemeral",
    text: `I have received the command \`\`\`${fullCommand}\`\`\` Please wait a moment.`
  };
  res.status(200).send(slackPayload);

  const featureName = matchGroup[1];
  const slicedGroup = matchGroup.slice(2);
  let subsequentUrlPath;
  if (slicedGroup.length === 0) {
    subsequentUrlPath = null;
  } else {
    subsequentUrlPath = `/${slicedGroup.join("/")}`;
  }

  const topicName = `slack-tools.${featureName}`;
  const messagePayload = {
    teamId: teamId,
    userId: userId,
    userName: userName,
    command: command,
    urlPath: subsequentUrlPath,
    text: text,
    responseUrl: responseUrl
  };
  publishMessage(topicName, messagePayload)
    .then((result) => {
      console.log("Publish message succeeded, result => ", result);
    })
    .catch((error) => {
      console.log("Publish message failed, error => ", error.message);
      const slackPayload = {
        response_type: "ephemeral",
        text: "Publish message failed. Please enter the slash command again. If the issue persists, please contact the Droidie maintenance personnel for assistance."
      };
      axios.post(responseUrl, slackPayload, {
        headers: { "Content-Type": "application/json" }
      }).then((response) => {
        console.log("Send Slack message succeeded, response => ", response.statusText);
      }).catch((error) => {
        console.log("Send Slack message failed, error => ", error.message);
      });
    });
});

function checkUrlPath(requestUrl) {
  const featureRegexp = [
    /^\/(build-strings)$/,
    /^\/(ci)\/([^/]+)$/
  ];
  const parsedUrl = url.parse(requestUrl, true);
  const parsedUrlPath = parsedUrl.path;
  let matchGroup;
  featureRegexp.every((item) => {
    matchGroup = parsedUrlPath.match(item);
    if (matchGroup !== null) {
      return false;
    }
    return true;
  });

  return [matchGroup, parsedUrlPath];
}

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