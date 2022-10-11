"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true,
});
Object.defineProperty(exports, "DiskCacheManager", {
  enumerable: true,
  get: function () {
    return _DiskCacheManager.DiskCacheManager;
  },
});
Object.defineProperty(exports, "ModuleMap", {
  enumerable: true,
  get: function () {
    return _ModuleMap.default;
  },
});
exports.DuplicateError =
  exports.default =
  exports.DuplicateHasteCandidatesError =
    void 0;

var _DiskCacheManager = require("./cache/DiskCacheManager");

var _constants = _interopRequireDefault(require("./constants"));

var _getMockName = _interopRequireDefault(require("./getMockName"));

var _HasteFS = _interopRequireDefault(require("./HasteFS"));

var _deepCloneInternalData = _interopRequireDefault(
  require("./lib/deepCloneInternalData")
);

var fastPath = _interopRequireWildcard(require("./lib/fast_path"));

var _getPlatformExtension = _interopRequireDefault(
  require("./lib/getPlatformExtension")
);

var _normalizePathSep = _interopRequireDefault(
  require("./lib/normalizePathSep")
);

var _rootRelativeCacheKeys = _interopRequireDefault(
  require("./lib/rootRelativeCacheKeys")
);

var _ModuleMap = _interopRequireDefault(require("./ModuleMap"));

var _FSEventsWatcher = _interopRequireDefault(
  require("./watchers/FSEventsWatcher")
);

var _NodeWatcher = _interopRequireDefault(require("./watchers/NodeWatcher"));

var _WatchmanWatcher = _interopRequireDefault(
  require("./watchers/WatchmanWatcher")
);

var _worker = require("./worker");

var _child_process = require("child_process");

var _events = _interopRequireDefault(require("events"));

var _invariant = _interopRequireDefault(require("invariant"));

var _jestRegexUtil = require("jest-regex-util");

var _jestWorker = require("jest-worker");

var path = _interopRequireWildcard(require("path"));

var _abortController = _interopRequireDefault(require("abort-controller"));

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
// $FlowFixMe[untyped-import] - it's a fork: https://github.com/facebook/jest/pull/10919
// $FlowFixMe[untyped-import] - WatchmanWatcher
// $FlowFixMe[untyped-import] - jest-regex-util
// $FlowFixMe[untyped-import] - jest-worker
// $FlowFixMe[untyped-import] - this is a polyfill
const nodeCrawl = require("./crawlers/node");

const watchmanCrawl = require("./crawlers/watchman");

const DuplicateHasteCandidatesError =
  _ModuleMap.default.DuplicateHasteCandidatesError;
exports.DuplicateHasteCandidatesError = DuplicateHasteCandidatesError;
// This should be bumped whenever a code change to `metro-file-map` itself
// would cause a change to the cache data structure and/or content (for a given
// filesystem state and build parameters).
const CACHE_BREAKER = "2";
const CHANGE_INTERVAL = 30;
const MAX_WAIT_TIME = 240000;
const NODE_MODULES = path.sep + "node_modules" + path.sep;
const PACKAGE_JSON = path.sep + "package.json";
const VCS_DIRECTORIES = [".git", ".hg"]
  .map((vcs) =>
    (0, _jestRegexUtil.escapePathForRegex)(path.sep + vcs + path.sep)
  )
  .join("|");

const canUseWatchman = (() => {
  try {
    (0, _child_process.execSync)("watchman --version", {
      stdio: ["ignore"],
    });
    return true;
  } catch {}

  return false;
})();
/**
 * HasteMap is a JavaScript implementation of Facebook's haste module system.
 *
 * This implementation is inspired by https://github.com/facebook/node-haste
 * and was built with for high-performance in large code repositories with
 * hundreds of thousands of files. This implementation is scalable and provides
 * predictable performance.
 *
 * Because the haste map creation and synchronization is critical to startup
 * performance and most tasks are blocked by I/O this class makes heavy use of
 * synchronous operations. It uses worker processes for parallelizing file
 * access and metadata extraction.
 *
 * The data structures created by `metro-file-map` can be used directly from the
 * cache without further processing. The metadata objects in the `files` and
 * `map` objects contain cross-references: a metadata object from one can look
 * up the corresponding metadata object in the other map. Note that in most
 * projects, the number of files will be greater than the number of haste
 * modules one module can refer to many files based on platform extensions.
 *
 * type HasteMap = {
 *   clocks: WatchmanClocks,
 *   files: {[filepath: string]: FileMetaData},
 *   map: {[id: string]: ModuleMapItem},
 *   mocks: {[id: string]: string},
 * }
 *
 * // Watchman clocks are used for query synchronization and file system deltas.
 * type WatchmanClocks = {[filepath: string]: string};
 *
 * type FileMetaData = {
 *   id: ?string, // used to look up module metadata objects in `map`.
 *   mtime: number, // check for outdated files.
 *   size: number, // size of the file in bytes.
 *   visited: boolean, // whether the file has been parsed or not.
 *   dependencies: Array<string>, // all relative dependencies of this file.
 *   sha1: ?string, // SHA-1 of the file, if requested via options.
 * };
 *
 * // Modules can be targeted to a specific platform based on the file name.
 * // Example: platform.ios.js and Platform.android.js will both map to the same
 * // `Platform` module. The platform should be specified during resolution.
 * type ModuleMapItem = {[platform: string]: ModuleMetaData};
 *
 * //
 * type ModuleMetaData = {
 *   path: string, // the path to look up the file object in `files`.
 *   type: string, // the module type (either `package` or `module`).
 * };
 *
 * Note that the data structures described above are conceptual only. The actual
 * implementation uses arrays and constant keys for metadata storage. Instead of
 * `{id: 'flatMap', mtime: 3421, size: 42, visited: true, dependencies: []}` the real
 * representation is similar to `['flatMap', 3421, 42, 1, []]` to save storage space
 * and reduce parse and write time of a big JSON blob.
 *
 * The HasteMap is created as follows:
 *  1. read data from the cache or create an empty structure.
 *
 *  2. crawl the file system.
 *     * empty cache: crawl the entire file system.
 *     * cache available:
 *       * if watchman is available: get file system delta changes.
 *       * if watchman is unavailable: crawl the entire file system.
 *     * build metadata objects for every file. This builds the `files` part of
 *       the `HasteMap`.
 *
 *  3. parse and extract metadata from changed files.
 *     * this is done in parallel over worker processes to improve performance.
 *     * the worst case is to parse all files.
 *     * the best case is no file system access and retrieving all data from
 *       the cache.
 *     * the average case is a small number of changed files.
 *
 *  4. serialize the new `HasteMap` in a cache file.
 *
 */

class HasteMap extends _events.default {
  static create(options) {
    return new HasteMap(options);
  } // $FlowFixMe[missing-local-annot]

  constructor(options) {
    var _options$dependencyEx, _options$watchmanDefe, _this$_options$perfLo;

    if (options.perfLogger) {
      var _options$perfLogger;

      (_options$perfLogger = options.perfLogger) === null ||
      _options$perfLogger === void 0
        ? void 0
        : _options$perfLogger.point("constructor_start");
    }

    super(); // Add VCS_DIRECTORIES to provided ignorePattern

    let ignorePattern;

    if (options.ignorePattern) {
      const inputIgnorePattern = options.ignorePattern;

      if (inputIgnorePattern instanceof RegExp) {
        ignorePattern = new RegExp(
          inputIgnorePattern.source.concat("|" + VCS_DIRECTORIES),
          inputIgnorePattern.flags
        );
      } else {
        throw new Error(
          "metro-file-map: the `ignorePattern` option must be a RegExp"
        );
      }
    } else {
      ignorePattern = new RegExp(VCS_DIRECTORIES);
    }

    const buildParameters = {
      computeDependencies:
        options.computeDependencies == null
          ? true
          : options.computeDependencies,
      computeSha1: options.computeSha1 || false,
      dependencyExtractor:
        (_options$dependencyEx = options.dependencyExtractor) !== null &&
        _options$dependencyEx !== void 0
          ? _options$dependencyEx
          : null,
      enableSymlinks: options.enableSymlinks || false,
      extensions: options.extensions,
      forceNodeFilesystemAPI: !!options.forceNodeFilesystemAPI,
      hasteImplModulePath: options.hasteImplModulePath,
      ignorePattern,
      mocksPattern:
        options.mocksPattern != null && options.mocksPattern !== ""
          ? new RegExp(options.mocksPattern)
          : null,
      platforms: options.platforms,
      retainAllFiles: options.retainAllFiles,
      rootDir: options.rootDir,
      roots: Array.from(new Set(options.roots)),
      skipPackageJson: !!options.skipPackageJson,
      cacheBreaker: CACHE_BREAKER,
    };
    this._options = {
      ...buildParameters,
      maxWorkers: options.maxWorkers,
      perfLogger: options.perfLogger,
      resetCache: options.resetCache,
      throwOnModuleCollision: !!options.throwOnModuleCollision,
      useWatchman: options.useWatchman == null ? true : options.useWatchman,
      watch: !!options.watch,
      watchmanDeferStates:
        (_options$watchmanDefe = options.watchmanDeferStates) !== null &&
        _options$watchmanDefe !== void 0
          ? _options$watchmanDefe
          : [],
    };
    this._console = options.console || global.console;
    this._cacheManager = options.cacheManagerFactory
      ? options.cacheManagerFactory.call(null, buildParameters)
      : new _DiskCacheManager.DiskCacheManager({
          buildParameters,
        });

    if (this._options.enableSymlinks && this._options.useWatchman) {
      throw new Error(
        "metro-file-map: enableSymlinks config option was set, but " +
          "is incompatible with watchman.\n" +
          "Set either `enableSymlinks` to false or `useWatchman` to false."
      );
    }

    this._buildPromise = null;
    this._watchers = [];
    this._worker = null;
    (_this$_options$perfLo = this._options.perfLogger) === null ||
    _this$_options$perfLo === void 0
      ? void 0
      : _this$_options$perfLo.point("constructor_end");
    this._crawlerAbortController = new _abortController.default();
  }

  static getCacheFilePath(cacheDirectory, cacheFilePrefix, buildParameters) {
    const { rootDirHash, relativeConfigHash } = (0,
    _rootRelativeCacheKeys.default)(buildParameters);
    return path.join(
      cacheDirectory,
      `${cacheFilePrefix}-${rootDirHash}-${relativeConfigHash}`
    );
  }

  static getModuleMapFromJSON(json) {
    return _ModuleMap.default.fromJSON(json);
  }

  getCacheFilePath() {
    if (!(this._cacheManager instanceof _DiskCacheManager.DiskCacheManager)) {
      throw new Error(
        "metro-file-map: getCacheFilePath is only supported when using DiskCacheManager"
      );
    }

    return this._cacheManager.getCacheFilePath();
  }

  build() {
    var _this$_options$perfLo2;

    (_this$_options$perfLo2 = this._options.perfLogger) === null ||
    _this$_options$perfLo2 === void 0
      ? void 0
      : _this$_options$perfLo2.point("build_start");

    if (!this._buildPromise) {
      this._buildPromise = (async () => {
        var _data$changedFiles, _data$removedFiles;

        const data = await this._buildFileMap(); // Persist when we don't know if files changed (changedFiles undefined)
        // or when we know a file was changed or deleted.

        let hasteMap;

        if (
          data.changedFiles == null ||
          data.changedFiles.size > 0 ||
          data.removedFiles.size > 0
        ) {
          hasteMap = await this._buildHasteMap(data);
        } else {
          hasteMap = data.hasteMap;
        }

        await this._persist(
          hasteMap,
          (_data$changedFiles = data.changedFiles) !== null &&
            _data$changedFiles !== void 0
            ? _data$changedFiles
            : new Map(),
          (_data$removedFiles = data.removedFiles) !== null &&
            _data$removedFiles !== void 0
            ? _data$removedFiles
            : new Map()
        );
        const rootDir = this._options.rootDir;
        const hasteFS = new _HasteFS.default({
          files: hasteMap.files,
          rootDir,
        });
        const moduleMap = new _ModuleMap.default({
          duplicates: hasteMap.duplicates,
          map: hasteMap.map,
          mocks: hasteMap.mocks,
          rootDir,
        });
        await this._watch(hasteMap);
        return {
          hasteFS,
          moduleMap,
        };
      })();
    }

    return this._buildPromise.then((result) => {
      var _this$_options$perfLo3;

      (_this$_options$perfLo3 = this._options.perfLogger) === null ||
      _this$_options$perfLo3 === void 0
        ? void 0
        : _this$_options$perfLo3.point("build_end");
      return result;
    });
  }
  /**
   * 1. read data from the cache or create an empty structure.
   */

  async read() {
    var _this$_options$perfLo4, _data, _this$_options$perfLo5;

    let data;
    (_this$_options$perfLo4 = this._options.perfLogger) === null ||
    _this$_options$perfLo4 === void 0
      ? void 0
      : _this$_options$perfLo4.point("read_start");

    try {
      data = await this._cacheManager.read();
    } catch {}

    data =
      (_data = data) !== null && _data !== void 0
        ? _data
        : this._createEmptyMap();
    (_this$_options$perfLo5 = this._options.perfLogger) === null ||
    _this$_options$perfLo5 === void 0
      ? void 0
      : _this$_options$perfLo5.point("read_end");
    return data;
  }

  async readModuleMap() {
    const data = await this.read();
    return new _ModuleMap.default({
      duplicates: data.duplicates,
      map: data.map,
      mocks: data.mocks,
      rootDir: this._options.rootDir,
    });
  }
  /**
   * 2. crawl the file system.
   */

  async _buildFileMap() {
    var _this$_options$perfLo6;

    let hasteMap;
    (_this$_options$perfLo6 = this._options.perfLogger) === null ||
    _this$_options$perfLo6 === void 0
      ? void 0
      : _this$_options$perfLo6.point("buildFileMap_start");

    try {
      hasteMap =
        this._options.resetCache === true
          ? this._createEmptyMap()
          : await this.read();
    } catch {
      hasteMap = this._createEmptyMap();
    }

    return this._crawl(hasteMap).then((result) => {
      var _this$_options$perfLo7;

      (_this$_options$perfLo7 = this._options.perfLogger) === null ||
      _this$_options$perfLo7 === void 0
        ? void 0
        : _this$_options$perfLo7.point("buildFileMap_end");
      return result;
    });
  }
  /**
   * 3. parse and extract metadata from changed files.
   */

  _processFile(hasteMap, map, mocks, filePath, workerOptions) {
    const rootDir = this._options.rootDir;

    const setModule = (id, module) => {
      let moduleMap = map.get(id);

      if (!moduleMap) {
        // $FlowFixMe[unclear-type] - Add type coverage
        moduleMap = Object.create(null);
        map.set(id, moduleMap);
      }

      const platform =
        (0, _getPlatformExtension.default)(
          module[_constants.default.PATH],
          this._options.platforms
        ) || _constants.default.GENERIC_PLATFORM;

      const existingModule = moduleMap[platform];

      if (
        existingModule &&
        existingModule[_constants.default.PATH] !==
          module[_constants.default.PATH]
      ) {
        const method = this._options.throwOnModuleCollision ? "error" : "warn";

        this._console[method](
          [
            "metro-file-map: Haste module naming collision: " + id,
            "  The following files share their name; please adjust your hasteImpl:",
            "    * <rootDir>" +
              path.sep +
              existingModule[_constants.default.PATH],
            "    * <rootDir>" + path.sep + module[_constants.default.PATH],
            "",
          ].join("\n")
        );

        if (this._options.throwOnModuleCollision) {
          throw new DuplicateError(
            existingModule[_constants.default.PATH],
            module[_constants.default.PATH]
          );
        } // We do NOT want consumers to use a module that is ambiguous.

        delete moduleMap[platform];

        if (Object.keys(moduleMap).length === 1) {
          map.delete(id);
        }

        let dupsByPlatform = hasteMap.duplicates.get(id);

        if (dupsByPlatform == null) {
          dupsByPlatform = new Map();
          hasteMap.duplicates.set(id, dupsByPlatform);
        }

        const dups = new Map([
          [module[_constants.default.PATH], module[_constants.default.TYPE]],
          [
            existingModule[_constants.default.PATH],
            existingModule[_constants.default.TYPE],
          ],
        ]);
        dupsByPlatform.set(platform, dups);
        return;
      }

      const dupsByPlatform = hasteMap.duplicates.get(id);

      if (dupsByPlatform != null) {
        const dups = dupsByPlatform.get(platform);

        if (dups != null) {
          dups.set(
            module[_constants.default.PATH],
            module[_constants.default.TYPE]
          );
        }

        return;
      }

      moduleMap[platform] = module;
    };

    const relativeFilePath = fastPath.relative(rootDir, filePath);
    const fileMetadata = hasteMap.files.get(relativeFilePath);

    if (!fileMetadata) {
      throw new Error(
        "metro-file-map: File to process was not found in the haste map."
      );
    }

    const moduleMetadata = hasteMap.map.get(
      fileMetadata[_constants.default.ID]
    );
    const computeSha1 =
      this._options.computeSha1 && !fileMetadata[_constants.default.SHA1]; // Callback called when the response from the worker is successful.

    const workerReply = (metadata) => {
      // `1` for truthy values instead of `true` to save cache space.
      fileMetadata[_constants.default.VISITED] = 1;
      const metadataId = metadata.id;
      const metadataModule = metadata.module;

      if (metadataId != null && metadataModule) {
        fileMetadata[_constants.default.ID] = metadataId;
        setModule(metadataId, metadataModule);
      }

      fileMetadata[_constants.default.DEPENDENCIES] = metadata.dependencies
        ? metadata.dependencies.join(_constants.default.DEPENDENCY_DELIM)
        : "";

      if (computeSha1) {
        fileMetadata[_constants.default.SHA1] = metadata.sha1;
      }
    }; // Callback called when the response from the worker is an error.

    const workerError = (error) => {
      if (
        error == null ||
        typeof error !== "object" ||
        error.message == null ||
        error.stack == null
      ) {
        // $FlowFixMe[reassign-const] - Refactor this
        error = new Error(error); // $FlowFixMe[incompatible-use] - error is mixed

        error.stack = ""; // Remove stack for stack-less errors.
      } // $FlowFixMe[incompatible-use] - error is mixed

      if (!["ENOENT", "EACCES"].includes(error.code)) {
        throw error;
      } // If a file cannot be read we remove it from the file list and
      // ignore the failure silently.

      hasteMap.files.delete(relativeFilePath);
    }; // If we retain all files in the virtual HasteFS representation, we avoid
    // reading them if they aren't important (node_modules).

    if (this._options.retainAllFiles && filePath.includes(NODE_MODULES)) {
      if (computeSha1) {
        return this._getWorker(workerOptions)
          .getSha1({
            computeDependencies: this._options.computeDependencies,
            computeSha1,
            dependencyExtractor: this._options.dependencyExtractor,
            filePath,
            hasteImplModulePath: this._options.hasteImplModulePath,
            rootDir,
          })
          .then(workerReply, workerError);
      }

      return null;
    }

    if (
      this._options.mocksPattern &&
      this._options.mocksPattern.test(filePath)
    ) {
      const mockPath = (0, _getMockName.default)(filePath);
      const existingMockPath = mocks.get(mockPath);

      if (existingMockPath != null) {
        const secondMockPath = fastPath.relative(rootDir, filePath);

        if (existingMockPath !== secondMockPath) {
          const method = this._options.throwOnModuleCollision
            ? "error"
            : "warn";

          this._console[method](
            [
              "metro-file-map: duplicate manual mock found: " + mockPath,
              "  The following files share their name; please delete one of them:",
              "    * <rootDir>" + path.sep + existingMockPath,
              "    * <rootDir>" + path.sep + secondMockPath,
              "",
            ].join("\n")
          );

          if (this._options.throwOnModuleCollision) {
            throw new DuplicateError(existingMockPath, secondMockPath);
          }
        }
      }

      mocks.set(mockPath, relativeFilePath);
    }

    if (fileMetadata[_constants.default.VISITED]) {
      if (!fileMetadata[_constants.default.ID]) {
        return null;
      }

      if (moduleMetadata != null) {
        const platform =
          (0, _getPlatformExtension.default)(
            filePath,
            this._options.platforms
          ) || _constants.default.GENERIC_PLATFORM;

        const module = moduleMetadata[platform];

        if (module == null) {
          return null;
        }

        const moduleId = fileMetadata[_constants.default.ID];
        let modulesByPlatform = map.get(moduleId);

        if (!modulesByPlatform) {
          // $FlowFixMe[unclear-type] - ModuleMapItem?
          modulesByPlatform = Object.create(null);
          map.set(moduleId, modulesByPlatform);
        }

        modulesByPlatform[platform] = module;
        return null;
      }
    }

    return this._getWorker(workerOptions)
      .worker({
        computeDependencies: this._options.computeDependencies,
        computeSha1,
        dependencyExtractor: this._options.dependencyExtractor,
        filePath,
        hasteImplModulePath: this._options.hasteImplModulePath,
        rootDir,
      })
      .then(workerReply, workerError);
  }

  _buildHasteMap(data) {
    var _this$_options$perfLo8;

    (_this$_options$perfLo8 = this._options.perfLogger) === null ||
    _this$_options$perfLo8 === void 0
      ? void 0
      : _this$_options$perfLo8.point("buildHasteMap_start");
    const { removedFiles, changedFiles, hasteMap } = data; // If any files were removed or we did not track what files changed, process
    // every file looking for changes. Otherwise, process only changed files.

    let map;
    let mocks;
    let filesToProcess;

    if (changedFiles == null || removedFiles.size) {
      map = new Map();
      mocks = new Map();
      filesToProcess = hasteMap.files;
    } else {
      map = hasteMap.map;
      mocks = hasteMap.mocks;
      filesToProcess = changedFiles;
    }

    for (const [relativeFilePath, fileMetadata] of removedFiles) {
      this._recoverDuplicates(
        hasteMap,
        relativeFilePath,
        fileMetadata[_constants.default.ID]
      );
    }

    const promises = [];

    for (const relativeFilePath of filesToProcess.keys()) {
      if (
        this._options.skipPackageJson &&
        relativeFilePath.endsWith(PACKAGE_JSON)
      ) {
        continue;
      } // SHA-1, if requested, should already be present thanks to the crawler.

      const filePath = fastPath.resolve(
        this._options.rootDir,
        relativeFilePath
      );

      const promise = this._processFile(hasteMap, map, mocks, filePath);

      if (promise) {
        promises.push(promise);
      }
    }

    return Promise.all(promises).then(
      () => {
        var _this$_options$perfLo9;

        this._cleanup();

        hasteMap.map = map;
        hasteMap.mocks = mocks;
        (_this$_options$perfLo9 = this._options.perfLogger) === null ||
        _this$_options$perfLo9 === void 0
          ? void 0
          : _this$_options$perfLo9.point("buildHasteMap_end");
        return hasteMap;
      },
      (error) => {
        this._cleanup();

        throw error;
      }
    );
  }

  _cleanup() {
    const worker = this._worker; // $FlowFixMe[prop-missing] - end is not on WorkerInterface

    if (worker && typeof worker.end === "function") {
      worker.end();
    }

    this._worker = null;
  }
  /**
   * 4. serialize the new `HasteMap` in a cache file.
   */

  async _persist(hasteMap, changed, removed) {
    var _this$_options$perfLo10, _this$_options$perfLo11;

    (_this$_options$perfLo10 = this._options.perfLogger) === null ||
    _this$_options$perfLo10 === void 0
      ? void 0
      : _this$_options$perfLo10.point("persist_start");
    const snapshot = (0, _deepCloneInternalData.default)(hasteMap);
    await this._cacheManager.write(snapshot, {
      changed,
      removed,
    });
    (_this$_options$perfLo11 = this._options.perfLogger) === null ||
    _this$_options$perfLo11 === void 0
      ? void 0
      : _this$_options$perfLo11.point("persist_end");
  }
  /**
   * Creates workers or parses files and extracts metadata in-process.
   */

  _getWorker(options) {
    if (!this._worker) {
      if ((options && options.forceInBand) || this._options.maxWorkers <= 1) {
        this._worker = {
          getSha1: _worker.getSha1,
          worker: _worker.worker,
        };
      } else {
        this._worker = new _jestWorker.Worker(require.resolve("./worker"), {
          exposedMethods: ["getSha1", "worker"],
          maxRetries: 3,
          numWorkers: this._options.maxWorkers,
        });
      }
    }

    return this._worker;
  }

  _crawl(hasteMap) {
    var _this$_options$perfLo12;

    (_this$_options$perfLo12 = this._options.perfLogger) === null ||
    _this$_options$perfLo12 === void 0
      ? void 0
      : _this$_options$perfLo12.point("crawl_start");
    const options = this._options;

    const ignore = (filePath) => this._ignore(filePath);

    const crawl =
      canUseWatchman && this._options.useWatchman ? watchmanCrawl : nodeCrawl;
    const crawlerOptions = {
      abortSignal: this._crawlerAbortController.signal,
      computeSha1: options.computeSha1,
      data: hasteMap,
      enableSymlinks: options.enableSymlinks,
      extensions: options.extensions,
      forceNodeFilesystemAPI: options.forceNodeFilesystemAPI,
      ignore,
      perfLogger: options.perfLogger,
      rootDir: options.rootDir,
      roots: options.roots,
    };

    const retry = (error) => {
      if (crawl === watchmanCrawl) {
        this._console.warn(
          "metro-file-map: Watchman crawl failed. Retrying once with node " +
            "crawler.\n" +
            "  Usually this happens when watchman isn't running. Create an " +
            "empty `.watchmanconfig` file in your project's root folder or " +
            "initialize a git or hg repository in your project.\n" +
            "  " +
            error.toString()
        );

        return nodeCrawl(crawlerOptions).catch((e) => {
          throw new Error(
            "Crawler retry failed:\n" +
              `  Original error: ${error.message}\n` +
              `  Retry error: ${e.message}\n`
          );
        });
      }

      throw error;
    };

    const logEnd = (result) => {
      var _this$_options$perfLo13;

      (_this$_options$perfLo13 = this._options.perfLogger) === null ||
      _this$_options$perfLo13 === void 0
        ? void 0
        : _this$_options$perfLo13.point("crawl_end");
      return result;
    };

    try {
      return crawl(crawlerOptions).catch(retry).then(logEnd);
    } catch (error) {
      return retry(error).then(logEnd);
    }
  }
  /**
   * Watch mode
   */

  _watch(hasteMap) {
    var _this$_options$perfLo14;

    (_this$_options$perfLo14 = this._options.perfLogger) === null ||
    _this$_options$perfLo14 === void 0
      ? void 0
      : _this$_options$perfLo14.point("watch_start");

    if (!this._options.watch) {
      var _this$_options$perfLo15;

      (_this$_options$perfLo15 = this._options.perfLogger) === null ||
      _this$_options$perfLo15 === void 0
        ? void 0
        : _this$_options$perfLo15.point("watch_end");
      return Promise.resolve();
    } // In watch mode, we'll only warn about module collisions and we'll retain
    // all files, even changes to node_modules.

    this._options.throwOnModuleCollision = false;
    this._options.retainAllFiles = true; // WatchmanWatcher > FSEventsWatcher > sane.NodeWatcher

    const WatcherImpl =
      canUseWatchman && this._options.useWatchman
        ? _WatchmanWatcher.default
        : _FSEventsWatcher.default.isSupported()
        ? _FSEventsWatcher.default
        : _NodeWatcher.default;
    const extensions = this._options.extensions;
    const ignorePattern = this._options.ignorePattern;
    const rootDir = this._options.rootDir;
    let changeQueue = Promise.resolve();
    let eventsQueue = []; // We only need to copy the entire haste map once on every "frame".

    let mustCopy = true;

    const createWatcher = (root) => {
      const watcher = new WatcherImpl(root, {
        dot: true,
        glob: [
          // Ensure we always include package.json files, which are crucial for
          /// module resolution.
          "**/package.json",
          ...extensions.map((extension) => "**/*." + extension),
        ],
        ignored: ignorePattern,
        watchmanDeferStates: this._options.watchmanDeferStates,
      });
      return new Promise((resolve, reject) => {
        const rejectTimeout = setTimeout(
          () => reject(new Error("Failed to start watch mode.")),
          MAX_WAIT_TIME
        );
        watcher.once("ready", () => {
          clearTimeout(rejectTimeout);
          watcher.on("all", onChange);
          resolve(watcher);
        });
      });
    };

    const emitChange = () => {
      if (eventsQueue.length) {
        mustCopy = true;
        const changeEvent = {
          eventsQueue,
          hasteFS: new _HasteFS.default({
            files: hasteMap.files,
            rootDir,
          }),
          moduleMap: new _ModuleMap.default({
            duplicates: hasteMap.duplicates,
            map: hasteMap.map,
            mocks: hasteMap.mocks,
            rootDir,
          }),
        };
        this.emit("change", changeEvent);
        eventsQueue = [];
      }
    };

    const onChange = (type, filePath, root, stat) => {
      const absoluteFilePath = path.join(
        root,
        (0, _normalizePathSep.default)(filePath)
      );

      if (
        (stat && stat.isDirectory()) ||
        this._ignore(absoluteFilePath) ||
        !extensions.some((extension) => absoluteFilePath.endsWith(extension))
      ) {
        return;
      }

      const relativeFilePath = fastPath.relative(rootDir, absoluteFilePath);
      const fileMetadata = hasteMap.files.get(relativeFilePath); // The file has been accessed, not modified

      if (
        type === "change" &&
        fileMetadata &&
        stat &&
        fileMetadata[_constants.default.MTIME] === stat.mtime.getTime()
      ) {
        return;
      }

      changeQueue = changeQueue
        .then(() => {
          // If we get duplicate events for the same file, ignore them.
          if (
            eventsQueue.find(
              (event) =>
                event.type === type &&
                event.filePath === absoluteFilePath &&
                ((!event.stat && !stat) ||
                  (!!event.stat &&
                    !!stat &&
                    event.stat.mtime.getTime() === stat.mtime.getTime()))
            )
          ) {
            return null;
          }

          if (mustCopy) {
            mustCopy = false; // $FlowFixMe[reassign-const] - Refactor this

            hasteMap = {
              clocks: new Map(hasteMap.clocks),
              duplicates: new Map(hasteMap.duplicates),
              files: new Map(hasteMap.files),
              map: new Map(hasteMap.map),
              mocks: new Map(hasteMap.mocks),
            };
          }

          const add = () => {
            eventsQueue.push({
              filePath: absoluteFilePath,
              stat,
              type,
            });
            return null;
          };

          const fileMetadata = hasteMap.files.get(relativeFilePath); // If it's not an addition, delete the file and all its metadata

          if (fileMetadata != null) {
            const moduleName = fileMetadata[_constants.default.ID];

            const platform =
              (0, _getPlatformExtension.default)(
                absoluteFilePath,
                this._options.platforms
              ) || _constants.default.GENERIC_PLATFORM;

            hasteMap.files.delete(relativeFilePath);
            let moduleMap = hasteMap.map.get(moduleName);

            if (moduleMap != null) {
              // We are forced to copy the object because metro-file-map exposes
              // the map as an immutable entity.
              moduleMap = Object.assign(Object.create(null), moduleMap);
              delete moduleMap[platform];

              if (Object.keys(moduleMap).length === 0) {
                hasteMap.map.delete(moduleName);
              } else {
                hasteMap.map.set(moduleName, moduleMap);
              }
            }

            if (
              this._options.mocksPattern &&
              this._options.mocksPattern.test(absoluteFilePath)
            ) {
              const mockName = (0, _getMockName.default)(absoluteFilePath);
              hasteMap.mocks.delete(mockName);
            }

            this._recoverDuplicates(hasteMap, relativeFilePath, moduleName);
          } // If the file was added or changed,
          // parse it and update the haste map.

          if (type === "add" || type === "change") {
            (0, _invariant.default)(
              stat,
              "since the file exists or changed, it should have stats"
            );
            const fileMetadata = [
              "",
              stat.mtime.getTime(),
              stat.size,
              0,
              "",
              null,
            ];
            hasteMap.files.set(relativeFilePath, fileMetadata);

            const promise = this._processFile(
              hasteMap,
              hasteMap.map,
              hasteMap.mocks,
              absoluteFilePath,
              {
                forceInBand: true,
              }
            ); // Cleanup

            this._cleanup();

            if (promise) {
              return promise.then(add);
            } else {
              // If a file in node_modules has changed,
              // emit an event regardless.
              add();
            }
          } else {
            add();
          }

          return null;
        })
        .catch((error) => {
          this._console.error(
            `metro-file-map: watch error:\n  ${error.stack}\n`
          );
        });
    };

    this._changeInterval = setInterval(emitChange, CHANGE_INTERVAL);
    return Promise.all(this._options.roots.map(createWatcher)).then(
      (watchers) => {
        var _this$_options$perfLo16;

        this._watchers = watchers;
        (_this$_options$perfLo16 = this._options.perfLogger) === null ||
        _this$_options$perfLo16 === void 0
          ? void 0
          : _this$_options$perfLo16.point("watch_end");
      }
    );
  }
  /**
   * This function should be called when the file under `filePath` is removed
   * or changed. When that happens, we want to figure out if that file was
   * part of a group of files that had the same ID. If it was, we want to
   * remove it from the group. Furthermore, if there is only one file
   * remaining in the group, then we want to restore that single file as the
   * correct resolution for its ID, and cleanup the duplicates index.
   */

  _recoverDuplicates(hasteMap, relativeFilePath, moduleName) {
    let dupsByPlatform = hasteMap.duplicates.get(moduleName);

    if (dupsByPlatform == null) {
      return;
    }

    const platform =
      (0, _getPlatformExtension.default)(
        relativeFilePath,
        this._options.platforms
      ) || _constants.default.GENERIC_PLATFORM;

    let dups = dupsByPlatform.get(platform);

    if (dups == null) {
      return;
    }

    dupsByPlatform = new Map(dupsByPlatform);
    hasteMap.duplicates.set(moduleName, dupsByPlatform);
    dups = new Map(dups);
    dupsByPlatform.set(platform, dups);
    dups.delete(relativeFilePath);

    if (dups.size !== 1) {
      return;
    }

    const uniqueModule = dups.entries().next().value;

    if (!uniqueModule) {
      return;
    }

    let dedupMap = hasteMap.map.get(moduleName);

    if (dedupMap == null) {
      // $FlowFixMe[unclear-type] - ModuleMapItem?
      dedupMap = Object.create(null);
      hasteMap.map.set(moduleName, dedupMap);
    }

    dedupMap[platform] = uniqueModule;
    dupsByPlatform.delete(platform);

    if (dupsByPlatform.size === 0) {
      hasteMap.duplicates.delete(moduleName);
    }
  }

  async end() {
    if (this._changeInterval) {
      clearInterval(this._changeInterval);
    }

    if (!this._watchers.length) {
      return;
    }

    await Promise.all(this._watchers.map((watcher) => watcher.close()));
    this._watchers = [];

    this._crawlerAbortController.abort();
  }
  /**
   * Helpers
   */

  _ignore(filePath) {
    const ignorePattern = this._options.ignorePattern;
    const ignoreMatched =
      ignorePattern instanceof RegExp
        ? ignorePattern.test(filePath)
        : ignorePattern && ignorePattern(filePath);
    return (
      ignoreMatched ||
      (!this._options.retainAllFiles && filePath.includes(NODE_MODULES))
    );
  }

  _createEmptyMap() {
    return {
      clocks: new Map(),
      duplicates: new Map(),
      files: new Map(),
      map: new Map(),
      mocks: new Map(),
    };
  }

  static H = _constants.default;
}

exports.default = HasteMap;

class DuplicateError extends Error {
  constructor(mockPath1, mockPath2) {
    super("Duplicated files or mocks. Please check the console for more info");
    this.mockPath1 = mockPath1;
    this.mockPath2 = mockPath2;
  }
}

exports.DuplicateError = DuplicateError;
