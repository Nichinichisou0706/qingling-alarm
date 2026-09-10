// Original, synthesized bell melody. Run with Node.js; no downloaded music is bundled.
const fs = require('fs');
const path = require('path');
const rate = 22050, seconds = 16, samples = rate * seconds;
const notes = [523.25,659.25,783.99,987.77,880,783.99,659.25,587.33,523.25,659.25,783.99,1046.5,987.77,783.99,659.25,523.25];
const pcm = Buffer.alloc(samples * 2);
for (let i=0;i<samples;i++) {
  const t=i/rate; let value=0;
  for(let n=Math.max(0,Math.floor(t)-3);n<=Math.min(15,Math.floor(t));n++) {
    const dt=t-n, f=notes[n];
    const env=Math.min(1,dt*35)*Math.exp(-dt*2.6);
    value+=(Math.sin(2*Math.PI*f*dt)+0.2*Math.sin(2*Math.PI*f*2*dt))*env*0.32;
  }
  value*=Math.min(1,t/0.5,(seconds-t)/0.5);
  pcm.writeInt16LE(Math.round(Math.max(-1,Math.min(1,value))*32767),i*2);
}
const header=Buffer.alloc(44);header.write('RIFF');header.writeUInt32LE(36+pcm.length,4);header.write('WAVEfmt ',8);header.writeUInt32LE(16,16);header.writeUInt16LE(1,20);header.writeUInt16LE(1,22);header.writeUInt32LE(rate,24);header.writeUInt32LE(rate*2,28);header.writeUInt16LE(2,32);header.writeUInt16LE(16,34);header.write('data',36);header.writeUInt32LE(pcm.length,40);
const dest=path.join(__dirname,'../app/src/main/res/raw/morning.wav');fs.mkdirSync(path.dirname(dest),{recursive:true});fs.writeFileSync(dest,Buffer.concat([header,pcm]));console.log('Generated original melody:',dest);
