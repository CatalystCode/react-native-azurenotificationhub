"use strict";

var _constants = _interopRequireDefault(require("../constants"));

var fastPath = _interopRequireWildcard(require("../lib/fast_path"));

var _normalizePathSep = _interopRequireDefault(
  require("../lib/normalizePathSep")
);

var path = _interopRequireWildcard(require("path"));

function _getRequireWildcardCache(nodeInterop) {
  if (typeof WeakMap !== "function") return null;
  var cacheBabelInterop = new WeakMap();
  var cacheNodeInterop = new WeakMap();
  return (_getRequireWildcardCache = function (nodeInterop) {
    return nodeInterop ? cacheNodeInterop : cacheBabelInterop;
  })(nodeInterop);
}

function _interopRequireWildcard(obj, nodeInterop) {
  if (!nodeInterop && obj && obj.__esModule) {
    return obj;
  }
  if (obj === null || (typeof obj !== "object" && typeof obj !== "function")) {
    return { default: obj };
  }
  var cache = _getRequireWildcardCache(nodeInterop);
  if (cache && cache.has(obj)) {
    return cache.get(obj);
  }
  var newObj = {};
  var hasPropertyDescriptor =
    Object.defineProperty && Object.getOwnPropertyDescriptor;
  for (var key in obj) {
    if (key !== "default" && Object.prototype.hasOwnProperty.call(obj, key)) {
      var desc = hasPropertyDescriptor
        ? Object.getOwnPropertyDescriptor(obj, key)
        : null;
      if (desc && (desc.get || desc.set)) {
        Object.defineProperty(newObj, key, desc);
      } else {
        newObj[key] = obj[key];
      }
    }
  }
  newObj.default = obj;
  if (cache) {
    cache.set(obj, newObj);
  }
  return newObj;
}

function _interopRequireDefault(obj) {
  return obj && obj.__esModule ? obj : { default: obj };
}

/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * @format
 */
const watchman = require("fb-watchman"); // $FlowFixMe[unclear-type] - Improve fb-watchman types to cover our uses

const WATCHMAN_WARNING_INITIAL_DELAY_MILLISECONDS = 10000;
const WATCHMAN_WARNING_INTERVAL_MILLISECONDS = 20000;
const watchmanURL = "https://facebook.github.io/watchman/docs/troubleshooting";

function makeWatchmanError(error) {
  error.message =
    `Watchman error: ${error.message.trim()}. Make sure watchman ` +
    `is running for this project. See ${watchmanURL}.`;
  return error;
}
/**
 * Wrap watchman capabilityCheck method as a promise.
 *
 * @param client watchman client
 * @param caps capabilities to verify
 * @returns a promise resolving to a list of verified capabilities
 */

async function capabilityCheck(client, caps) {
  return new Promise((resolve, reject) => {
    client.capabilityCheck(
      // @ts-expect-error: incorrectly typed
      caps,
      (error, response) => {
        if (error != null || response == null) {
          reject(
            error !== null && error !== void 0
              ? error
              : new Error("capabilityCheck: Response missing")
          );
        } else {
          resolve(response);
        }
      }
    );
  });
}

module.exports = async function watchmanCrawl(options) {
  var _options$abortSignal;

  const fields = ["name", "exists", "mtime_ms", "size"];
  const { data, extensions, ignore, rootDir, roots, perfLogger } = options;
  const clocks = data.clocks;
  perfLogger === null || perfLogger === void 0
    ? void 0
    : perfLogger.point("watchmanCrawl_start");
  const client = new watchman.Client();
  (_options$abortSignal = options.abortSignal) === null ||
  _options$abortSignal === void 0
    ? void 0
    : _options$abortSignal.addEventListener("abort", () => client.end());
  perfLogger === null || perfLogger === void 0
    ? void 0
    : perfLogger.point("watchmanCrawl/negotiateCapabilities_start"); // https://facebook.github.io/watchman/docs/capabilities.html
  // Check adds about ~28ms

  const capabilities = await capabilityCheck(client, {
    // If a required capability is missing then an error will be thrown,
    // we don't need this assertion, so using optional instead.
    optional: ["suffix-set"],
  });
  const suffixExpression =
    capabilities !== null &&
    capabilities !== void 0 &&
    capabilities.capabilities["suffix-set"] // If available, use the optimized `suffix-set` operation:
      ? // https://facebook.github.io/watchman/docs/expr/suffix.html#suffix-set
        ["suffix", extensions]
      : ["anyof", ...extensions.map((extension) => ["suffix", extension])];
  let clientError; // $FlowFixMe[prop-missing] - Client is not typed as an EventEmitter

  client.on("error", (error) => (clientError = makeWatchmanError(error)));
  let didLogWatchmanWaitMessage = false; // $FlowFixMe[unclear-type] - Fix to use fb-watchman types

  const cmd = async (command, ...args) => {
    const logWatchmanWaitMessage = () => {
      didLogWatchmanWaitMessage = true;
      console.warn(`Waiting for Watchman (${command})...`);
    };

    let intervalOrTimeoutId = setTimeout(() => {
      logWatchmanWaitMessage();
      intervalOrTimeoutId = setInterval(
        logWatchmanWaitMessage,
        WATCHMAN_WARNING_INTERVAL_MILLISECONDS
      );
    }, WATCHMAN_WARNING_INITIAL_DELAY_MILLISECONDS);

    try {
      return await new Promise(
        (
          resolve,
          reject // $FlowFixMe[incompatible-call] - dynamic call of command
        ) =>
          client.command([command, ...args], (error, result) =>
            error ? reject(makeWatchmanError(error)) : resolve(result)
          )
      );
    } finally {
      // $FlowFixMe[incompatible-call] clearInterval / clearTimeout are interchangeable
      clearInterval(intervalOrTimeoutId);
    }
  };

  if (options.computeSha1) {
    const { capabilities } = await cmd("list-capabilities");

    if (capabilities.indexOf("field-content.sha1hex") !== -1) {
      fields.push("content.sha1hex");
    }
  }

  perfLogger === null || perfLogger === void 0
    ? void 0
    : perfLogger.point("watchmanCrawl/negotiateCapabilities_end");

  async function getWatchmanRoots(roots) {
    perfLogger === null || perfLogger === void 0
      ? void 0
      : perfLogger.point("watchmanCrawl/getWatchmanRoots_start");
    const watchmanRoots = new Map();
    await Promise.all(
      roots.map(async (root, index) => {
        perfLogger === null || perfLogger === void 0
          ? void 0
          : perfLogger.point(`watchmanCrawl/watchProject_${index}_start`);
        const response = await cmd("watch-project", root);
        perfLogger === null || perfLogger === void 0
          ? void 0
          : perfLogger.point(`watchmanCrawl/watchProject_${index}_end`);
        const existing = watchmanRoots.get(response.watch); // A root can only be filtered if it was never seen with a
        // relative_path before.

        const canBeFiltered = !existing || existing.length > 0;

        if (canBeFiltered) {
          if (response.relative_path) {
            watchmanRoots.set(
              response.watch,
              (existing || []).concat(response.relative_path)
            );
          } else {
            // Make the filter directories an empty array to signal that this
            // root was already seen and needs to be watched for all files or
            // directories.
            watchmanRoots.set(response.watch, []);
          }
        }
      })
    );
    perfLogger === null || perfLogger === void 0
      ? void 0
      : perfLogger.point("watchmanCrawl/getWatchmanRoots_end");
    return watchmanRoots;
  }

  async function queryWatchmanForDirs(rootProjectDirMappings) {
    perfLogger === null || perfLogger === void 0
      ? void 0
      : perfLogger.point("watchmanCrawl/queryWatchmanForDirs_start");
    const results = new Map();
    let isFresh = false;
    await Promise.all(
      Array.from(rootProjectDirMappings).map(
        async ([root, directoryFilters], index) => {
          var _since$scm;

          // Jest is only going to store one type of clock; a string that
          // represents a local clock. However, the Watchman crawler supports
          // a second type of clock that can be written by automation outside of
          // Jest, called an "scm query", which fetches changed files based on
          // source control mergebases. The reason this is necessary is because
          // local clocks are not portable across systems, but scm queries are.
          // By using scm queries, we can create the haste map on a different
          // system and import it, transforming the clock into a local clock.
          const since = clocks.get(fastPath.relative(rootDir, root));
          perfLogger === null || perfLogger === void 0
            ? void 0
            : perfLogger.annotate({
                bool: {
                  [`watchmanCrawl/query_${index}_has_clock`]: since != null,
                },
              });
          const query = {
            fields,
            expression: [
              "allof", // Match regular files only. Different Watchman generators treat
              // symlinks differently, so this ensures consistent results.
              ["type", "f"],
            ],
          };
          /**
           * Watchman "query planner".
           *
           * Watchman file queries consist of 1 or more generators that feed
           * files through the expression evaluator.
           *
           * Strategy:
           * 1. Select the narrowest possible generator so that the expression
           *    evaluator has fewer candidates to process.
           * 2. Evaluate expressions from narrowest to broadest.
           * 3. Don't use an expression to recheck a condition that the
           *    generator already guarantees.
           * 4. Compose expressions to avoid combinatorial explosions in the
           *    number of terms.
           *
           * The ordering of generators/filters, from narrow to broad, is:
           * - since          = O(changes)
           * - glob / dirname = O(files in a subtree of the repo)
           * - suffix         = O(files in the repo)
           *
           * We assume that file extensions are ~uniformly distributed in the
           * repo but Haste map projects are focused on a handful of
           * directories. Therefore `glob` < `suffix`.
           */

          let queryGenerator;

          if (since != null) {
            // Use the `since` generator and filter by both path and extension.
            query.since = since;
            queryGenerator = "since";
            query.expression.push(
              ["anyof", ...directoryFilters.map((dir) => ["dirname", dir])],
              suffixExpression
            );
          } else if (directoryFilters.length > 0) {
            // Use the `glob` generator and filter only by extension.
            query.glob = directoryFilters.map((directory) => `${directory}/**`);
            query.glob_includedotfiles = true;
            queryGenerator = "glob";
            query.expression.push(suffixExpression);
          } else {
            // Use the `suffix` generator with no path/extension filtering.
            query.suffix = extensions;
            queryGenerator = "suffix";
          }

          perfLogger === null || perfLogger === void 0
            ? void 0
            : perfLogger.annotate({
                string: {
                  [`watchmanCrawl/query_${index}_generator`]: queryGenerator,
                },
              });
          perfLogger === null || perfLogger === void 0
            ? void 0
            : perfLogger.point(`watchmanCrawl/query_${index}_start`);
          const response = await cmd("query", root, query);
          perfLogger === null || perfLogger === void 0
            ? void 0
            : perfLogger.point(`watchmanCrawl/query_${index}_end`);

          if ("warning" in response) {
            console.warn("watchman warning: ", response.warning);
          } // When a source-control query is used, we ignore the "is fresh"
          // response from Watchman because it will be true despite the query
          // being incremental.

          const isSourceControlQuery =
            typeof since !== "string" &&
            (since === null || since === void 0
              ? void 0
              : (_since$scm = since.scm) === null || _since$scm === void 0
              ? void 0
              : _since$scm["mergebase-with"]) != null;

          if (!isSourceControlQuery) {
            isFresh = isFresh || response.is_fresh_instance;
          }

          results.set(root, response);
        }
      )
    );
    perfLogger === null || perfLogger === void 0
      ? void 0
      : perfLogger.point("watchmanCrawl/queryWatchmanForDirs_end");
    return {
      isFresh,
      results,
    };
  }

  let files = data.files;
  let removedFiles = new Map();
  const changedFiles = new Map();
  let results;
  let isFresh = false;
  let queryError;

  try {
    const watchmanRoots = await getWatchmanRoots(roots);
    const watchmanFileResults = await queryWatchmanForDirs(watchmanRoots); // Reset the file map if watchman was restarted and sends us a list of
    // files.

    if (watchmanFileResults.isFresh) {
      files = new Map();
      removedFiles = new Map(data.files);
      isFresh = true;
    }

    results = watchmanFileResults.results;
  } catch (e) {
    queryError = e;
  }

  client.end();

  if (results == null) {
    var _ref, _queryError;

    if (clientError) {
      var _clientError$message;

      perfLogger === null || perfLogger === void 0
        ? void 0
        : perfLogger.annotate({
            string: {
              "watchmanCrawl/client_error":
                (_clientError$message = clientError.message) !== null &&
                _clientError$message !== void 0
                  ? _clientError$message
                  : "[message missing]",
            },
          });
    }

    if (queryError) {
      var _queryError$message;

      perfLogger === null || perfLogger === void 0
        ? void 0
        : perfLogger.annotate({
            string: {
              "watchmanCrawl/query_error":
                (_queryError$message = queryError.message) !== null &&
                _queryError$message !== void 0
                  ? _queryError$message
                  : "[message missing]",
            },
          });
    }

    perfLogger === null || perfLogger === void 0
      ? void 0
      : perfLogger.point("watchmanCrawl_end");
    throw (_ref =
      (_queryError = queryError) !== null && _queryError !== void 0
        ? _queryError
        : clientError) !== null && _ref !== void 0
      ? _ref
      : new Error("Watchman file results missing");
  }

  perfLogger === null || perfLogger === void 0
    ? void 0
    : perfLogger.point("watchmanCrawl/processResults_start");

  for (const [watchRoot, response] of results) {
    const fsRoot = (0, _normalizePathSep.default)(watchRoot);
    const relativeFsRoot = fastPath.relative(rootDir, fsRoot);
    clocks.set(
      relativeFsRoot, // Ensure we persist only the local clock.
      typeof response.clock === "string" ? response.clock : response.clock.clock
    );

    for (const fileData of response.files) {
      const filePath =
        fsRoot + path.sep + (0, _normalizePathSep.default)(fileData.name);
      const relativeFilePath = fastPath.relative(rootDir, filePath);
      const existingFileData = data.files.get(relativeFilePath); // If watchman is fresh, the removed files map starts with all files
      // and we remove them as we verify they still exist.

      if (isFresh && existingFileData && fileData.exists) {
        removedFiles.delete(relativeFilePath);
      }

      if (!fileData.exists) {
        // No need to act on files that do not exist and were not tracked.
        if (existingFileData) {
          files.delete(relativeFilePath); // If watchman is not fresh, we will know what specific files were
          // deleted since we last ran and can track only those files.

          if (!isFresh) {
            removedFiles.set(relativeFilePath, existingFileData);
          }
        }
      } else if (!ignore(filePath)) {
        const mtime =
          typeof fileData.mtime_ms === "number"
            ? fileData.mtime_ms
            : fileData.mtime_ms.toNumber();
        const size = fileData.size;
        let sha1hex = fileData["content.sha1hex"];

        if (typeof sha1hex !== "string" || sha1hex.length !== 40) {
          sha1hex = undefined;
        }

        let nextData;

        if (
          existingFileData &&
          existingFileData[_constants.default.MTIME] === mtime
        ) {
          nextData = existingFileData;
        } else if (
          existingFileData &&
          sha1hex != null &&
          existingFileData[_constants.default.SHA1] === sha1hex
        ) {
          nextData = [
            existingFileData[0],
            mtime,
            existingFileData[2],
            existingFileData[3],
            existingFileData[4],
            existingFileData[5],
          ];
        } else {
          var _sha1hex;

          // See ../constants.ts
          nextData = [
            "",
            mtime,
            size,
            0,
            "",
            (_sha1hex = sha1hex) !== null && _sha1hex !== void 0
              ? _sha1hex
              : null,
          ];
        }

        files.set(relativeFilePath, nextData);
        changedFiles.set(relativeFilePath, nextData);
      }
    }
  }

  data.files = files;
  perfLogger === null || perfLogger === void 0
    ? void 0
    : perfLogger.point("watchmanCrawl/processResults_end");
  perfLogger === null || perfLogger === void 0
    ? void 0
    : perfLogger.point("watchmanCrawl_end");

  if (didLogWatchmanWaitMessage) {
    console.warn("Watchman query finished.");
  }

  return {
    changedFiles: isFresh ? undefined : changedFiles,
    hasteMap: data,
    removedFiles,
  };
};
