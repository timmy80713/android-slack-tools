"use strict";

const axios = require("axios");
const functions = require("@google-cloud/functions-framework");
const slack = require("./slack");

functions.http("main", (req, res) => {
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

    const slackPayload = {
        response_type: "ephemeral",
        text: "Please wait a moment."
    };
    res.status(200).send(slackPayload);

    fetchBranches(1)
        .then((branches) => {
            const slackPayload = generateSlackPayload(branches);
            console.log("Slack payload => ", JSON.stringify(slackPayload));
            axios.post(req.body.response_url, JSON.stringify(slackPayload), {
                headers: { "Content-Type": "application/json" }
            }).then((response) => {
                console.log("Send Slack message succeeded => ", response.statusText);
            }).catch((error) => {
                console.log("Send Slack message failed => ", error.message);
            });
        })
        .catch((error) => {
            console.log("Fetch branches failed => ", error.message);
        });
});

function fetchBranches(page, branches = []) {
    const githubData = JSON.parse(process.env.GITHUB_DATA)
    const owner = githubData.owner
    const repo = githubData.repo
    return axios.get(`https://api.github.com/repos/${owner}/${repo}/branches`, {
        params: { page: page, per_page: 30 },
        headers: { Authorization: `token ${process.env.GITHUB_PERSONAL_ACCESS_TOKEN}` }
    }).then((response) => {
        const data = response.data;
        branches.push(...data);
        if (data.length === 0 || data.length % 30 !== 0) {
            return branches;
        } else {
            return fetchBranches(page + 1, branches);
        }
    }).catch((error) => {
        console.log("Fetch GutHub branches failed => ", error.message);
        return branches;
    });
}

function generateSlackPayload(branches) {
    const gitBranchBlock = generateGitBranchBlock(branches);
    const gitTagBlock = generateGitTagBlock();
    const gitCommitHashBlock = generateGitCommitHashBlock();
    const variantBlocks = generateVariantBlocks();
    const messageBlock = generateMessageBlock();
    const submitBlock = generateSubmitBlock();
    const slackBlocks = [gitBranchBlock]
        .concat([gitTagBlock])
        .concat([gitCommitHashBlock])
        .concat(variantBlocks)
        .concat([messageBlock])
        .concat([submitBlock]);
    return { blocks: slackBlocks };
}
function generateGitBranchBlock(branches) {
    const staticSelectBranchOptions = branches.map((branch) => ({
        text: {
            type: "plain_text",
            text: branch.name.length > 70 ? `${branch.name.slice(0, 70)}...` : branch.name
        },
        value: `${branch.name}`
    }));
    return {
        type: "input",
        element: {
            type: "static_select",
            placeholder: {
                type: "plain_text",
                text: "Select a Git branch"
            },
            options: staticSelectBranchOptions,
            action_id: "action_id_git_branch"
        },
        label: {
            type: "plain_text",
            text: "Git branch"
        },
        block_id: "block_id_git_branch"
    };
}

function generateGitTagBlock() {
    return {
        type: "input",
        element: {
            type: "plain_text_input",
            placeholder: {
                type: "plain_text",
                text: "e.g. 1.0.0"
            },
            action_id: "action_id_git_tag"
        },
        label: {
            type: "plain_text",
            text: "Git tag (Optional)"
        },
        block_id: "block_id_git_tag"
    };
}

function generateGitCommitHashBlock() {
    return {
        type: "input",
        element: {
            type: "plain_text_input",
            placeholder: {
                type: "plain_text",
                text: "e.g. a7de8e3606c1ea8961b3497c862dc33c641d65b5"
            },
            action_id: "action_id_git_commit_hash"
        },
        label: {
            type: "plain_text",
            text: "Git commit hash (Optional)"
        },
        block_id: "block_id_git_commit_hash"
    };
}

function generateVariantBlocks() {
    const variantBlocks = [];
    for (let i = 1; i < 4; i++) {
        variantBlocks.push(generateVariantLabelBlock(i));
        variantBlocks.push(generateVariantSelectorBlock(i));
    }
    return variantBlocks;
}

function generateVariantLabelBlock(index) {
    return {
        type: "section",
        text: {
            type: "plain_text",
            text: `variant${index}`
        },
        block_id: `block_id_variant_${index}_label`
    };
}

function generateVariantSelectorBlock(index) {
    const apps = [
        { key: "Taiwan", value: "Taiwan" },
        { key: "Taiwan X", value: "taiwanFreedom" },
        { key: "overseas", value: "overseas" }
    ];
    const staticSelectAppOptions = apps.map((app) => ({
        text: {
            type: "plain_text",
            text: `${app.key}`
        },
        value: `${app.value}`
    }));

    const environments = ["Dev", "Production"];
    const staticSelectEnvironmentOptions = environments.map((environment) => ({
        text: {
            type: "plain_text",
            text: `${environment}`
        },
        value: `${environment}`
    }));

    const buildTypes = ["Debug", "Release"];
    const staticSelectBuildTypeOptions = buildTypes.map((buildType) => ({
        text: {
            type: "plain_text",
            text: `${buildType}`
        },
        value: `${buildType}`
    }));

    return {
        type: "actions",
        elements: [
            {
                type: "static_select",
                placeholder: {
                    type: "plain_text",
                    text: "Select a app"
                },
                options: staticSelectAppOptions,
                action_id: `action_id_variant_${index}_app`
            },
            {
                type: "static_select",
                placeholder: {
                    type: "plain_text",
                    text: "Select a environment"
                },
                options: staticSelectEnvironmentOptions,
                action_id: `action_id_variant_${index}_environment`
            },
            {
                type: "static_select",
                placeholder: {
                    type: "plain_text",
                    text: "Select a build type"
                },
                options: staticSelectBuildTypeOptions,
                action_id: `action_id_variant_${index}_build_type`
            }
        ],
        block_id: `block_id_variant_${index}_selector`
    };
}

function generateMessageBlock() {
    return {
        type: "input",
        element: {
            type: "plain_text_input",
            action_id: "action_id_message"
        },
        label: {
            type: "plain_text",
            text: "Message (Optional)"
        },
        block_id: "block_id_message"
    };
}

function generateSubmitBlock() {
    return {
        type: "actions",
        elements: [
            {
                type: "button",
                text: {
                    type: "plain_text",
                    text: "submit"
                },
                value: "submit",
                style: "primary",
                action_id: "action_id_submit"
            }
        ],
        block_id: "block_id_submit"
    };
}