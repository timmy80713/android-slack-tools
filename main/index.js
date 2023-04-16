"use strict";

const axios = require("axios");
const functions = require("@google-cloud/functions-framework");
const { PubSub } = require("@google-cloud/pubsub");
const url = require("url");
const slack = require("./slack");

const pubsub = new PubSub();

functions.http("useTool", (req, res) => {
  console.log("The service has started.");

  try {
    slack.verify(req);
  } catch (error) {
    console.log("Slack verify failed.", error.message);
    res.status(200).send(error.message);
    return;
  }

  let formattedRequestBody;
  if (isFromSlackAction(req)) {
    const payload = JSON.parse(req.body.payload);
    if (!isSubmitAction(payload)) {
      console.log("Not yet submitted.");
      res.status(200).send("");
      return;
    }
    formattedRequestBody = convertPayloadToFormattedRequestBody(payload);
    const slackPayload = { text: "Got it, please wait a moment." };
    axios.post(formattedRequestBody.response_url, slackPayload, {
      headers: { "Content-Type": "application/json" }
    }).then((response) => {
      console.log("Send Slack message succeeded => ", response.statusText);
    }).catch((error) => {
      console.log("Send Slack message failed => ", error.message);
    });
  } else {
    formattedRequestBody = req.body;
  }

  handleRequest(req, res, formattedRequestBody);
});

function isFromSlackAction(req) {
  return "payload" in req.body;
}

function isSubmitAction(payload) {
  return payload.actions[0].action_id === "action_id_submit";
}

function convertPayloadToFormattedRequestBody(payload) {
  const values = payload.state.values;
  const branchOption = values.block_id_branch.action_id_branch.selected_option;
  const branch = branchOption ? branchOption.value || "" : "";
  const branchArg = branch ? `-b ${branch} ` : "";

  const variants = [];
  for (let i = 1; i < 4; i++) {
    const selector = values[`block_id_variant_${i}_selector`];

    const appKey = `action_id_variant_${i}_app`;
    const environmentKey = `action_id_variant_${i}_environment`;
    const buildTypeKey = `action_id_variant_${i}_build_type`;

    const appOption = selector[appKey].selected_option;
    const app = appOption ? appOption.value || "" : "";

    const environmentOption = selector[environmentKey].selected_option;
    const environment = environmentOption ? environmentOption.value || "" : "";

    const buildTypeOption = selector[buildTypeKey].selected_option;
    const buildType = buildTypeOption ? buildTypeOption.value || "" : "";
    variants.push(app + environment + buildType);
  }

  const variantString = variants.filter(Boolean).join(",");
  const variantStringArg = variantString ? `-v ${variantString} ` : "";

  const message = values.block_id_message.action_id_message.value || "";
  const messageArg = message ? `-m "${message}"` : "";

  const text = branchArg + variantStringArg + messageArg;
  console.log(text);

  return {
    token: payload.token,
    team_id: payload.team.id,
    team_domain: payload.team.domain,
    channel_id: payload.channel.id,
    channel_name: payload.channel.name,
    user_id: payload.user.id,
    user_name: payload.user.name,
    command: "/timmy-a",
    text: text,
    api_app_id: payload.api_app_id,
    is_enterprise_install: payload.is_enterprise_install,
    response_url: payload.response_url,
    trigger_id: payload.trigger_id
  };
}

function handleRequest(req, res, formattedRequestBody) {
  console.log("Request from ==> ", req.body.team_domain, req.body.channel_name, req.body.user_name)
  console.log("Command ==> ", req.body.command)
  console.log("Text ==> ", req.body.text)

  const teamId = formattedRequestBody.team_id;
  if (teamId !== "T03180XEC") {
    const errorMessage = "You cannot use this slash command in this Slack team.";
    console.log(errorMessage);
    res.status(200).send(errorMessage);
    return;
  }

  const channelId = formattedRequestBody.channel_id;
  const allowChannelIds = ["D025JAVQRRT", "C053SNHM2NM"];
  if (!allowChannelIds.includes(channelId)) {
    const errorMessage = "You cannot use this slash command in this channel.";
    console.log(errorMessage);
    res.status(200).send(errorMessage);
    return;
  }

  const userId = formattedRequestBody.user_id;
  const userName = formattedRequestBody.user_name;
  const command = formattedRequestBody.command;
  const text = formattedRequestBody.text;
  const responseUrl = formattedRequestBody.response_url;
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
      console.log("Publish message succeeded => ", result);
    })
    .catch((error) => {
      console.log("Publish message failed => ", error.message);
      const slackPayload = {
        response_type: "ephemeral",
        text: "Publish message failed. Please enter the slash command again. If the issue persists, please contact the Droidie maintenance personnel for assistance."
      };
      axios.post(responseUrl, slackPayload, {
        headers: { "Content-Type": "application/json" }
      }).then((response) => {
        console.log("Send Slack message succeeded => ", response.statusText);
      }).catch((error) => {
        console.log("Send Slack message failed => ", error.message);
      });
    });
}

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