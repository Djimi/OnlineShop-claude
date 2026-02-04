import { countTotalSymbols } from "./dpm-helper.js";

const log = console.log;

log("Hello, welcome to the Node.js playground!");

const totalSymbols = countTotalSymbols("dpm", 123, "hello");
log(`Total symbols: ${totalSymbols}`);

const numbers = [1, 2];

let [a, b] = numbers;

log(`a: ${a}, b: ${b}`);
log(`Sum: ${a + b}`);

let [one, two, three] = new Set([1, 2, 3, 6, 333]);

log(`one: ${one}, two: ${two}, three: ${three}. Sum: ${one + two + three}`);