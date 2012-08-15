package com.mcgill.clientdist;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

import com.mcgill.clientdist.ClientDistActivity;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class SoundCapture implements Runnable {
	private Handler handler; //handler to send data to the UI
	private ClientDistActivity activity; //object to connect to the main activity
	
	 //audio definition
	final int sampleRate = 44100,
			  channelConfig = AudioFormat.CHANNEL_IN_MONO, 
			  audioFormat = AudioFormat.ENCODING_PCM_16BIT,
			  fftPoints = 64, //fft points
			  bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
	final int shortBufferSize = (audioFormat == AudioFormat.ENCODING_PCM_16BIT ? bufferSize/2 : bufferSize);
	
    short buffer[] = new short[shortBufferSize]; //buffer to store the audio
	float bufferFloat[] = new float[fftPoints * 2], //buffer to pass the audio for the fft
		  magnitude[] = new float[fftPoints]; //final array, with the magnitudes/frequency
    
	private FloatFFT_1D fft = new FloatFFT_1D(fftPoints); //object to perform the fft
    
    private AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);
    private boolean recording = false;

    private long counter = 0;
    private long receiveTime = 0;
    
    public SoundCapture(ClientDistActivity parent) {
    	activity = parent;
        handler = new Handler();
    }
    
	@Override
	public void run() {
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		start();
		
		while (recording) {
			recorder.read(buffer,0,shortBufferSize);
			int j = 0;
			long avg = 0;
			int avgcounter = 0;
			for (j = 0; j < shortBufferSize; j += fftPoints) {
				int end = j+fftPoints;
				if (end > shortBufferSize)
					break;
				
				//prepare the float buffer to be passed to the FFT operation
				for (int i = j, k = 0; i < end; i++, k+=2) {
					bufferFloat[k] = (float) buffer[i]; //real part
					bufferFloat[k+1] = 0;				//imaginary part
				}
				
				//perform the FFT
				fft.complexForward(bufferFloat);

				//convert the FFT values from Im/Real to an absolute value
				for (int i = 0, k = 0; i < fftPoints; i++, k+=2) {
					magnitude[i] = (float) Math.sqrt(bufferFloat[k]*bufferFloat[k] + bufferFloat[k+1]*bufferFloat[k+1]);
				}
				
				if (
						magnitude[27] > 4*magnitude[24] &&
						magnitude[27] > 4*magnitude[25] &&
						magnitude[27] > 4*magnitude[26] &&
						magnitude[27] > 4*magnitude[28] &&
						magnitude[27] > 4*magnitude[29] &&
						magnitude[27] > 4*magnitude[30]
					)
				{
//					if (counter > 50 && counter < 200) {
						avg += (long) magnitude[27];
						avgcounter ++;
//					}
//					Log.e("", "" + counter);
//					counter = 0;
				}
//				else if (counter >= 0) {
//					counter++;
//				}
			}
			if (avgcounter > 0)
				postToActivity((int) (avg/avgcounter));
		}
	}
	
	public void stop() {
		if (recorder == null)
			return;
		recorder.stop();
		recorder.release();
		recorder = null;
		recording = false;
		//Log.e("Recorder", "Stopping");
	}
	
	public void pause() {
		if (recorder == null)
			return;
		recorder.stop();
		recording = false;
		//Log.e("Recorder", "Stopping");
	}
	
	public void start() {
		if (recorder == null) {
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);
		}
		recording = true;
		recorder.startRecording();
	}
	
	private void postToActivity(final int time) {
		handler.post(new Runnable() {
			public void run() {
				activity.receivedSoundSignal(time);
			}
		});
    }
	
	public void prepareToReceive(long timems) {
		counter = -1;
		receiveTime = timems;
	}
}
