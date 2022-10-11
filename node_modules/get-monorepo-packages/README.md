# `get-monorepo-packages`

[![Travis](https://img.shields.io/travis/azz/get-monorepo-packages.svg?style=flat-square)](https://travis-ci.org/azz/get-monorepo-packages)
[![Prettier](https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square)](https://github.com/prettier/prettier)
[![npm](https://img.shields.io/npm/v/get-monorepo-packages.svg?style=flat-square)](https://npmjs.org/get-monorepo-packages)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg?style=flat-square)](https://github.com/semantic-release/semantic-release)
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](LICENSE)

Get a list of packages from a monorepo. Supports:

* [Lerna](https://github.com/lerna/lerna)
* [Yarn workspaces](https://yarnpkg.com/lang/en/docs/workspaces/)
* [Bolt](http://boltpkg.com/)

## Install

```bash
npm install --save get-monorepo-packages
```

## Usage

```js
import getPackages from 'get-monorepo-packages';
getPackages('/path/to/root');
```

Returns an array of objects containing:

* `location` - The relative path to the package.
* `package` - The `package.json` file for the package.
