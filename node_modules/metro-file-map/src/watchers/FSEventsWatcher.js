"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true,
});
exports.default = void 0;

var _anymatch = _interopRequireDefault(require("anymatch"));

var _events = _interopRequireDefault(require("events"));

var fs = _interopRequireWildcard(require("graceful-fs"));

var path = _interopRequireWildcard(require("path"));

var _walker = _interopRequireDefault(require("walker"));

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
 * @format
 *
 */
// $FlowFixMe[untyped-import] - anymatch
// $FlowFixMe[untyped-import] - walker
// $FlowFixMe[untyped-import] - micromatch
const micromatch = require("micromatch");

// $FlowFixMe[unclear-type] - fsevents
let fsevents = null;

try {
  // $FlowFixMe[cannot-resolve-module] - Optional, Darwin only
  fsevents = require("fsevents");
} catch {
  // Optional dependency, only supported on Darwin.
}

const CHANGE_EVENT = "change";
const DELETE_EVENT = "delete";
const ADD_EVENT = "add";
const ALL_EVENT = "all";

/**
 * Export `FSEventsWatcher` class.
 * Watches `dir`.
 */
class FSEventsWatcher extends _events.default {
  static isSupported() {
    return fsevents != null;
  }

  static _normalizeProxy(callback) {
    return (filepath, stats) => callback(path.normalize(filepath), stats);
  }

  static _recReaddir(
    dir,
    dirCallback,
    fileCallback, // $FlowFixMe[unclear-type] Add types for callback
    endCallback, // $FlowFixMe[unclear-type] Add types for callback
    errorCallback,
    ignored
  ) {
    (0, _walker.default)(dir)
      .filterDir(
        (currentDir) => !ignored || !(0, _anymatch.default)(ignored, currentDir)
      )
      .on("dir", FSEventsWatcher._normalizeProxy(dirCallback))
      .on("file", FSEventsWatcher._normalizeProxy(fileCallback))
      .on("error", errorCallback)
      .on("end", () => {
        endCallback();
      });
  }

  constructor(
    dir,
    opts // $FlowFixMe[missing-local-annot]
  ) {
    if (!fsevents) {
      throw new Error(
        "`fsevents` unavailable (this watcher can only be used on Darwin)"
      );
    }

    super();
    this.dot = opts.dot || false;
    this.ignored = opts.ignored;
    this.glob = Array.isArray(opts.glob) ? opts.glob : [opts.glob];
    this.hasIgnore =
      Boolean(opts.ignored) && !(Array.isArray(opts) && opts.length > 0);
    this.doIgnore = opts.ignored
      ? (0, _anymatch.default)(opts.ignored)
      : () => false;
    this.root = path.resolve(dir);
    this.fsEventsWatchStopper = fsevents.watch(
      this.root, // $FlowFixMe[method-unbinding] - Refactor
      this._handleEvent.bind(this)
    );
    this._tracked = new Set();

    FSEventsWatcher._recReaddir(
      this.root,
      (filepath) => {
        this._tracked.add(filepath);
      },
      (filepath) => {
        this._tracked.add(filepath);
      }, // $FlowFixMe[method-unbinding] - Refactor
      this.emit.bind(this, "ready"), // $FlowFixMe[method-unbinding] - Refactor
      this.emit.bind(this, "error"),
      this.ignored
    );
  }
  /**
   * End watching.
   */

  async close(callback) {
    await this.fsEventsWatchStopper();
    this.removeAllListeners();

    if (typeof callback === "function") {
      // $FlowFixMe[extra-arg] - Is this a Node-style callback or as typed?
      process.nextTick(callback.bind(null, null, true));
    }
  }

  _isFileIncluded(relativePath) {
    if (this.doIgnore(relativePath)) {
      return false;
    }

    return this.glob.length
      ? micromatch([relativePath], this.glob, {
          dot: this.dot,
        }).length > 0
      : this.dot || micromatch([relativePath], "**/*").length > 0;
  }

  _handleEvent(filepath) {
    const relativePath = path.relative(this.root, filepath);

    if (!this._isFileIncluded(relativePath)) {
      return;
    }

    fs.lstat(filepath, (error, stat) => {
      if (error && error.code !== "ENOENT") {
        this.emit("error", error);
        return;
      }

      if (error) {
        // Ignore files that aren't tracked and don't exist.
        if (!this._tracked.has(filepath)) {
          return;
        }

        this._emit(DELETE_EVENT, relativePath);

        this._tracked.delete(filepath);

        return;
      }

      if (this._tracked.has(filepath)) {
        this._emit(CHANGE_EVENT, relativePath, stat);
      } else {
        this._tracked.add(filepath);

        this._emit(ADD_EVENT, relativePath, stat);
      }
    });
  }
  /**
   * Emit events.
   */

  _emit(type, file, stat) {
    this.emit(type, file, this.root, stat);
    this.emit(ALL_EVENT, type, file, this.root, stat);
  }
}

exports.default = FSEventsWatcher;
