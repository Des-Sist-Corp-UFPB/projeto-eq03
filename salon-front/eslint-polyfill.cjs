const util = require('util');

if (typeof util.styleText !== 'function') {
  util.styleText = function(format, text) {
    const codes = {
      red: '\x1b[31m',
      green: '\x1b[32m',
      yellow: '\x1b[33m',
      blue: '\x1b[34m',
      magenta: '\x1b[35m',
      cyan: '\x1b[36m',
      gray: '\x1b[90m',
      bold: '\x1b[1m',
      underline: '\x1b[4m',
      dim: '\x1b[2m',
      reset: '\x1b[0m'
    };
    const formats = Array.isArray(format) ? format : [format];
    let start = '';
    let end = '';
    formats.forEach(f => {
      if (codes[f]) {
        start += codes[f];
        end = codes.reset + end;
      }
    });
    return start + text + end;
  };
}
