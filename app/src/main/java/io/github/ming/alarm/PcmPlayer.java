package io.github.ming.alarm;

import android.content.Context;
import android.media.*;
import android.os.*;
import java.io.File;
import java.nio.*;
import java.util.Locale;

/** Decode imports to stereo PCM and stream AudioTrack; avoids vendor compressed-audio offload. */
public final class PcmPlayer {
    public interface Listener {
        void ready(PcmPlayer sender);
        void complete(PcmPlayer sender);
        void error(PcmPlayer sender,String reason);
        void route(AudioDeviceInfo device);
    }
    private final Context context;
    private final File file;
    private final Listener listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Object gate=new Object();
    private volatile boolean closed,paused=true;
    private volatile AudioTrack track;
    private volatile AudioDeviceInfo preferred;
    private volatile float volume=0.7f;
    private Thread thread;
    private int rate=44100,channels=2,encoding=AudioFormat.ENCODING_PCM_16BIT;
    private long frames,lastReport;
    private float peak;

    public PcmPlayer(Context context,File file,Listener listener){this.context=context.getApplicationContext();this.file=file;this.listener=listener;}
    public void prepare(){thread=new Thread(this::decode,"Qingling-PcmDecode");thread.start();}
    public void setVolume(float value){volume=value;AudioTrack t=track;if(t!=null)try{t.setVolume(value);}catch(IllegalStateException ignored){}}
    public boolean setPreferredDevice(AudioDeviceInfo device){
        preferred=device;AudioTrack t=track;
        return t==null||t.setPreferredDevice(device);
    }
    public AudioDeviceInfo getRoutedDevice(){AudioTrack t=track;return t==null?null:t.getRoutedDevice();}
    public void play(){
        paused=false;AudioTrack t=track;if(t!=null)t.play();
        synchronized(gate){gate.notifyAll();}
    }
    public void pause(){paused=true;AudioTrack t=track;if(t!=null)try{t.pause();}catch(IllegalStateException ignored){}}
    public void close(){
        closed=true;paused=false;synchronized(gate){gate.notifyAll();}
        AudioTrack t=track;if(t!=null)try{t.pause();t.flush();t.stop();}catch(IllegalStateException ignored){}
        if(thread!=null)thread.interrupt();
    }
    private void awaitPlaying() throws InterruptedException {
        synchronized(gate){while(paused&&!closed)gate.wait();}
    }
    private void configure(MediaFormat format){
        rate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE);channels=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        encoding=format.containsKey(MediaFormat.KEY_PCM_ENCODING)?format.getInteger(MediaFormat.KEY_PCM_ENCODING):AudioFormat.ENCODING_PCM_16BIT;
        if(channels<1||channels>8)throw new IllegalArgumentException("不支持的音频声道数："+channels);
        AudioTrack previous=track;if(previous!=null){previous.stop();previous.release();track=null;}
        int minimum=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
        if(minimum<0)throw new IllegalStateException("系统不支持该采样率："+rate);
        AudioAttributes.Builder attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
        if(Build.VERSION.SDK_INT>=32)attributes.setSpatializationBehavior(AudioAttributes.SPATIALIZATION_BEHAVIOR_NEVER);
        AudioTrack created=new AudioTrack.Builder()
            .setAudioAttributes(attributes.build())
            .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setTransferMode(AudioTrack.MODE_STREAM).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_NONE)
            .setBufferSizeInBytes(Math.max(minimum*4,32768)).build();
        track=created;
        created.setVolume(volume);created.setPreferredDevice(preferred);
        created.addOnRoutingChangedListener(router->{if(!closed)listener.route(created.getRoutedDevice());},main);
        main.post(()->{if(!closed)listener.ready(this);});
    }
    private float sample(ByteBuffer buffer){
        if(encoding==AudioFormat.ENCODING_PCM_16BIT)return buffer.getShort()/32768f;
        if(encoding==AudioFormat.ENCODING_PCM_FLOAT)return buffer.getFloat();
        if(encoding==AudioFormat.ENCODING_PCM_8BIT)return ((buffer.get()&255)-128)/128f;
        if(Build.VERSION.SDK_INT>=31&&encoding==AudioFormat.ENCODING_PCM_24BIT_PACKED){
            int v=(buffer.get()&255)|((buffer.get()&255)<<8)|(buffer.get()<<16);return v/8388608f;
        }
        if(Build.VERSION.SDK_INT>=31&&encoding==AudioFormat.ENCODING_PCM_32BIT)return buffer.getInt()/2147483648f;
        throw new IllegalArgumentException("不支持的 PCM 格式："+encoding);
    }
    private int bytesPerSample(){
        if(encoding==AudioFormat.ENCODING_PCM_8BIT)return 1;
        if(encoding==AudioFormat.ENCODING_PCM_16BIT)return 2;
        if(Build.VERSION.SDK_INT>=31&&encoding==AudioFormat.ENCODING_PCM_24BIT_PACKED)return 3;
        return 4;
    }
    private void write(ByteBuffer buffer) throws Exception {
        int count=buffer.remaining()/(channels*bytesPerSample());byte[] stereo=new byte[count*4];
        ByteBuffer out=ByteBuffer.wrap(stereo).order(ByteOrder.LITTLE_ENDIAN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for(int frame=0;frame<count;frame++){
            float left=sample(buffer),right=channels==1?left:sample(buffer);
            for(int ch=2;ch<channels;ch++){float extra=sample(buffer);left+=extra*0.5f;right+=extra*0.5f;}
            if(channels>2){float divisor=1+(channels-2)*0.5f;left/=divisor;right/=divisor;}
            if(!Float.isFinite(left))left=0;if(!Float.isFinite(right))right=0;
            peak=Math.max(peak,Math.max(Math.abs(left),Math.abs(right)));
            out.putShort((short)(Math.max(-1,Math.min(1,left))*32767));out.putShort((short)(Math.max(-1,Math.min(1,right))*32767));
        }
        int offset=0;
        while(offset<stereo.length&&!closed){
            awaitPlaying();if(closed)return;
            int n=track.write(stereo,offset,stereo.length-offset,AudioTrack.WRITE_BLOCKING);
            if(n<0)throw new IllegalStateException("PCM 输出失败："+n);
            if(n==0){Thread.sleep(10);continue;}offset+=n;
        }
        frames+=count;
        if(frames-lastReport>=rate){
            lastReport=frames;
            String status=String.format(Locale.CHINA,"PCM 立体声 · %d Hz · 已解码 %d 秒 · 峰值 %.1f%%",rate,frames/rate,peak*100);
            Store.prefs(context).edit().putString("audioDecode",status).apply();
            if(frames<rate*3L)android.util.Log.i("QinglingAudio",status);
        }
    }
    private void decode(){
        MediaExtractor extractor=new MediaExtractor();MediaCodec codec=null;
        try{
            extractor.setDataSource(file.getAbsolutePath());int audio=-1;
            for(int i=0;i<extractor.getTrackCount();i++){String mime=extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith("audio/")){audio=i;break;}}
            if(audio<0)throw new IllegalArgumentException("文件中没有可解码音频");
            extractor.selectTrack(audio);MediaFormat input=extractor.getTrackFormat(audio);
            String mime=input.getString(MediaFormat.KEY_MIME);
            // WAV / raw PCM needs no compressed decoder.
            if("audio/raw".equals(mime)){
                configure(input);ByteBuffer raw=ByteBuffer.allocateDirect(256*1024);
                while(!closed){raw.clear();int size=extractor.readSampleData(raw,0);if(size<0)break;raw.position(0);raw.limit(size);write(raw);extractor.advance();}
            }else{
                input.setInteger(MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT);
                codec=MediaCodec.createDecoderByType(mime);codec.configure(input,null,null,0);codec.start();
                boolean inputEnd=false,outputEnd=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
                while(!closed&&!outputEnd){
                    if(!inputEnd){
                        int index=codec.dequeueInputBuffer(10_000);
                        if(index>=0){ByteBuffer b=codec.getInputBuffer(index);int n=extractor.readSampleData(b,0);
                            if(n<0){codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}
                            else{codec.queueInputBuffer(index,0,n,Math.max(0,extractor.getSampleTime()),0);extractor.advance();}}
                    }
                    int index=codec.dequeueOutputBuffer(info,10_000);
                    if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)configure(codec.getOutputFormat());
                    else if(index>=0){
                        ByteBuffer b=codec.getOutputBuffer(index);
                        if(info.size>0&&b!=null){if(track==null)configure(codec.getOutputFormat());b.position(info.offset);b.limit(info.offset+info.size);write(b);}
                        outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(index,false);
                    }
                }
            }
            // Let the streaming buffer drain before advancing to the next song.
            long deadline=SystemClock.elapsedRealtime()+5000;
            while(!closed&&track!=null&&(track.getPlaybackHeadPosition()&0xffffffffL)<frames&&SystemClock.elapsedRealtime()<deadline)Thread.sleep(20);
            if(!closed)main.post(()->{if(!closed)listener.complete(this);});
        }catch(Exception e){if(!closed)main.post(()->{if(!closed)listener.error(this,e.getClass().getSimpleName()+": "+e.getMessage());});}
        finally{
            if(codec!=null){try{codec.stop();}catch(Exception ignored){}codec.release();}
            extractor.release();AudioTrack t=track;track=null;if(t!=null){try{t.stop();}catch(Exception ignored){}t.release();}
        }
    }
}
