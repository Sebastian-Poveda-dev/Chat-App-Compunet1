package com.chatapp.client.model;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.sound.sampled.*;

public class AudioRecorder {
    
    private AudioFormat audioFormat;
    private TargetDataLine microphoneLine;
    private ByteArrayOutputStream recordedData;
    private Thread recordingThread;
    private boolean isRecording;


    public AudioRecorder() {
        audioFormat = new AudioFormat(16000, 16, 1, true, true);
        recordedData = new ByteArrayOutputStream();
        isRecording = false;
    }



    public void startRecording() {
        try {
            microphoneLine = AudioSystem.getTargetDataLine(audioFormat);
            microphoneLine.open(audioFormat);
            microphoneLine.start();

            isRecording = true;

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (isRecording) {
                    int bytesRead = microphoneLine.read(buffer, 0, buffer.length);

                    // Apply gain reduction to prevent saturation (reduce volume by 70%)
                    applyGainReduction(buffer, bytesRead, 0.07);
                    
                    recordedData.write(buffer, 0, bytesRead);
                }
            });
            recordingThread.start();

        } catch (LineUnavailableException e) {
            System.out.println("Mic not available.");
            e.printStackTrace();
        }
    }

    /**
     * Applies gain reduction to audio samples to prevent saturation
     * @param buffer The audio buffer (16-bit big-endian samples)
     * @param length Number of valid bytes in the buffer
     * @param gain Gain multiplier (0.0 to 1.0). Use 0.5 to reduce volume by 50%
     */
    private void applyGainReduction(byte[] buffer, int length, double gain) {
        // Process samples in pairs (16-bit = 2 bytes)
        for (int i = 0; i < length - 1; i += 2) {
            // Read 16-bit sample (big-endian format)
            int sample = (buffer[i] << 8) | (buffer[i + 1] & 0xFF);
            
            // Apply gain
            sample = (int)(sample * gain);
            
            // Clamp to prevent overflow
            if (sample > 32767) sample = 32767;
            if (sample < -32768) sample = -32768;
            
            // Write back to buffer
            buffer[i] = (byte)((sample >> 8) & 0xFF);
            buffer[i + 1] = (byte)(sample & 0xFF);
        }
    }

    public void stopRecording() {
        isRecording = false;
        if (microphoneLine != null) {
            microphoneLine.stop();
            microphoneLine.close();
        }
    }

    public byte[] getAudioBytes() {
        try {
            byte[] audioData = recordedData.toByteArray();
            
            if (audioData.length == 0) {
                System.out.println("Warning: No audio data recorded in getAudioBytes()");
                return new byte[0];
            }
            
            byte[] wavHeader = createWavHeader(audioData.length);
            
            // Combine header and audio data
            byte[] completeWavFile = new byte[wavHeader.length + audioData.length];
            System.arraycopy(wavHeader, 0, completeWavFile, 0, wavHeader.length);
            System.arraycopy(audioData, 0, completeWavFile, wavHeader.length, audioData.length);
            
            System.out.println("getAudioBytes() - Header size: " + wavHeader.length + " bytes");
            System.out.println("getAudioBytes() - Audio data size: " + audioData.length + " bytes");
            System.out.println("getAudioBytes() - Total WAV file size: " + completeWavFile.length + " bytes");
            
            return completeWavFile;
        } catch (Exception e) {
            System.out.println("Error in getAudioBytes(): " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Resets the recording buffer for a new recording
     */
    public void resetRecording() {
        recordedData.reset();
    }

    public String saveAudio() {

        try {
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
            String fileName = "audio_" + timestamp + ".wav";
    
            File audioFile = new File("audios/sent/" + fileName);
            audioFile.getParentFile().mkdirs(); // Creates directories if not exist
    
            byte[] audioData = recordedData.toByteArray();
            
            if (audioData.length == 0) {
                System.out.println("Error: No audio data recorded");
                return null;
            }
    
            byte[] wavHeader = createWavHeader(audioData.length);
    
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(wavHeader);
            fos.write(audioData);
            fos.close();
            
            System.out.println("Audio saved: " + audioFile.getAbsolutePath());
            System.out.println("Audio data size: " + audioData.length + " bytes");
            System.out.println("Total file size (with header): " + audioFile.length() + " bytes");
    
            return fileName;
        } catch (IOException e) {
            System.out.println("Error saving audio");
            e.printStackTrace();
            return null;
        }

    }


    private byte[] createWavHeader(int audioDataSize) {
        
        int sampleRate = 16000;
        int bitsPerSample = 16;
        int channels = 1;
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        int blockAlign = channels * (bitsPerSample / 8);
        
        byte[] header = new byte[44];
        
        // RIFF header
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        
        // Tamaño total del archivo - 8 bytes
        int fileSize = audioDataSize + 36;
        header[4] = (byte) (fileSize & 0xff);
        header[5] = (byte) ((fileSize >> 8) & 0xff);
        header[6] = (byte) ((fileSize >> 16) & 0xff);
        header[7] = (byte) ((fileSize >> 24) & 0xff);
        
        // WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        
        // fmt subchunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        
        // Tamaño del formato (16 para PCM)
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        
        // Formato de audio (1 = PCM)
        header[20] = 1;
        header[21] = 0;
        
        // Número de canales
        header[22] = (byte) channels;
        header[23] = 0;
        
        // Sample rate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        
        // Byte rate
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        
        // Block align
        header[32] = (byte) blockAlign;
        header[33] = 0;
        
        // Bits per sample
        header[34] = (byte) bitsPerSample;
        header[35] = 0;
        
        // data subchunk
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        
        // Tamaño de los datos
        header[40] = (byte) (audioDataSize & 0xff);
        header[41] = (byte) ((audioDataSize >> 8) & 0xff);
        header[42] = (byte) ((audioDataSize >> 16) & 0xff);
        header[43] = (byte) ((audioDataSize >> 24) & 0xff);
        
        return header;
    
    }
    

}
