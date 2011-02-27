/*
 * Camundo <http://www.camundo..com> Copyright (C) 2011  Wouter Van der Beken.
 *
 * This file is part of Camundo.
 *
 * Camundo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Camundo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Camundo.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.camundo.media;

import java.io.IOException;
import java.io.InputStream;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;



public class FFMPEGRtmpPublisher extends Thread{

	
	private String socketAddress;
	private FFMPEGInputPipe pipe;
	
	
	private LocalSocket receiver;
	private boolean up = false;
	
	
    
    public FFMPEGRtmpPublisher( String socketAddress, FFMPEGInputPipe pipe ) {
    	this.socketAddress = socketAddress;
    	this.pipe = pipe;
    	
    }
    
    private static final int BUFFER_LENGTH = 64;//for AMR audio this is ok?
    
    
    @Override
    public void run() {
        try {
            LocalServerSocket server = new LocalServerSocket(socketAddress);
            pipe.start();
            
            while( !pipe.processRunning() ) {
            	Log.i("CameraCaptureServer", "[ run() ] pipe not yet running, waiting.");
            	try {
            		Thread.sleep(250);
            	}
            	catch( Exception e) {
            		e.printStackTrace();
            	}
            }
            //write bootstrap
            pipe.writeBootstrap();
            
            // its up
            up = true;
            while (up) {
                
                receiver = server.accept();
                
                receiver.setReceiveBufferSize(BUFFER_LENGTH);
                receiver.setSendBufferSize(BUFFER_LENGTH);
                
                
                if (receiver != null) {
                    InputStream input = receiver.getInputStream();
                    Log.i("FFMPEGRtmpPublisher", "[ run() ] input [" + input + "]");
                    
                    int read = -1;
                    int available;
                    byte[] buffer = new byte[BUFFER_LENGTH];
                    
                    while ( (read = input.read()) != -1) {
                    	//Log.d("CameraCaptureServer", "[ run() ] read [" + read + "] buffer empty [" + input.available() + "] receiver [" + receiver + "]" );
                    	pipe.write(read);
                    	while( (available = input.available()) > 0 ) {
                    		if ( available > BUFFER_LENGTH ) {
                    			available = BUFFER_LENGTH;
                    		}
                   			input.read(buffer, 0, available);
                   			pipe.write(buffer, 0, available);
                    	}
                    }
                }
                else {
                	Log.i("FFMPEGRtmpPublisher", "[ run() ] receiver is null!!!");
                }
            }
            if ( pipe != null ) {
            	Log.i("FFMPEGRtmpPublisher", "[ run() ] closing pipe");
            	pipe.close();
            }
            else {
            	Log.i("FFMPEGRtmpPublisher", "[ run() ] closing pipe not necessary, is already null");
            }
            
            if ( receiver != null ) {
            	Log.i("FFMPEGRtmpPublisher", "[ run() ] closing receiver");
            	receiver.close();
            }
            else {
            	Log.i("FFMPEGRtmpPublisher", "[ run() ] closing receiver not necessary, is already null");
            }
            
            Log.i("FFMPEGRtmpPublisher", "[ run() ] closing server");
            server.close();
        } 
        catch (IOException e) {
        	e.printStackTrace();
            Log.e(getClass().getName(), e.getMessage());
        }
        Log.i("FFMPEGRtmpPublisher", "[ run() ] done");
    }
    
    
    public void shutdown(){
    	Log.i("FFMPEGRtmpPublisher", "[ shutdown() ] up is false");
    	up = false;
    }

    
    public boolean up() {
    	return up;
    }
    

}