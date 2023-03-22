"use strict";

const functions = require("@google-cloud/functions-framework");

functions.http("useTool", (req, res) => {
  res.status(200).send("謝謝你 9527");
});