"use strict";

require("dotenv").config();
const express = require("express");
const app = express();

// Application endpoints
const routes = require("./routes");
routes(app);

// Server listening on port from environment variables
const port = process.env.SERVER_PORT || 8080;
app.use(express.json());
app.listen(port);
console.log(`${process.env.APPLICATION_NAME} started on port ${port}`);

// {{< product-c8y-iot >}} and Slack controllers
require("./controllers");