const fs = require('fs');

const code = fs.readFileSync('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'utf8');
const lines = code.split('\n');

let balance = 0;
for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    for (const char of line) {
        if (char === '{') balance++;
        if (char === '}') balance--;
    }
}
console.log("Final balance:", balance);
