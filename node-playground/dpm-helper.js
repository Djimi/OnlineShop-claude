export function countTotalSymbols(...args) {
  return args.reduce((sum, value) => sum + String(value).length, 0);
}
