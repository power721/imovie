export function truncate (text, length, clamp) {
  if (!text) {
    return ''
  }

  clamp = clamp || '...'
  length = length || 650

  if (text.length <= length) {
    return text
  }

  let tcText = text.slice(0, length - clamp.length)
  let last = tcText.length - 1

  while (last > 0 && tcText[last] !== ' ' && tcText[last] !== clamp[0]) {
    last -= 1
  }

  // Fix for case when text dont have any `space`
  last = last || length - clamp.length

  tcText = tcText.slice(0, last)

  return tcText + clamp
}

export function date (value) {
  var dateObj = new Date(value)
  var month = dateObj.getUTCMonth() + 1
  var day = dateObj.getUTCDate()
  var year = dateObj.getUTCFullYear()
  return year + '-' + month + '-' + day
}

export function join (values, delm) {
  if (!values) {
    return ''
  }

  delm = delm || ' / '
  return values.map(function (elem) {
    return elem.name || elem
  }).join(delm)
}

export function imdb (value) {
  return value.replace('http://www.imdb.com/title/', '')
}
