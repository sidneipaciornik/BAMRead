/*
 ***********************************************************************
 *                                                                     *
 * BAM image format reader (.db and .dd files)                         *
 *                                                                     *
 ***********************************************************************
 */

import java.io.*;
import java.util.*;

import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


/** This plugin reads BAM image formats (.db and .dd files) **/
public class BAMRead_ extends ImagePlus implements PlugIn {

	private static final String BAMRead_VERSION = "November 2010 BAM-CT-Group";

	// Supported types
	String[] types = { "DD", "DB" };
	String[] typesDescription = { "BAM-CT Data Format", "BAM-CT Image Format" };

	public void run(String arg) {
		String directory, fileName, type = "BAM-CT Data";
		OpenDialog od;
		FileOpener fileOpener;
		File f;
		StringTokenizer st;
		int datatype = 0;
		boolean update = false;
		int oldnImages;

		// Show about box if called from ij.properties
		if (arg.equals("about")) {
			showAbout();
			return;
		}
		// Open file
		od = new OpenDialog("Open " + type + "...", arg);
		directory = od.getDirectory();
		fileName = od.getFileName();
		if (fileName == null)
			return;
		// IJ.write("Opening "+type+" image "+directory+fileName);
		// Otherwise this goes to the Results window
		IJ.showStatus("Opening " + type + " image " + directory + fileName);
		f = new File(directory + fileName);

		// The following code is "borrowed" from ij.plugin.Raw
		FileInfo fileInfo = new FileInfo();
		fileInfo.fileFormat = fileInfo.RAW;
		fileInfo.fileName = fileName;
		fileInfo.directory = directory;
		fileInfo.gapBetweenImages = 0;
		fileInfo.whiteIsZero = false;

		datatype = parseBAMDataFormatHeader(type, f, fileInfo);

		int framestart = 1;
		int framenumber = fileInfo.nImages;


		if (fileInfo.nImages > 1) {
			GenericDialog gd = new GenericDialog("Select Frames");
			gd.addNumericField("First Frame: ", framestart, 0);
			gd.addNumericField("Number of Frames: ", framenumber, 0);
			gd.showDialog();
			
			if (gd.wasCanceled()) {
				return ;	
			}	
			framestart=(int)gd.getNextNumber();
			framenumber=(int)gd.getNextNumber();
		}

                // special values
		if (framestart == 0 && framenumber == 0) {
			framestart = fileInfo.nImages;
			framenumber = 1;
			update = true;
		}
		if (framestart > 0 && framenumber == 0) {
			framenumber = fileInfo.nImages - framestart + 1;
                }
		if (framestart == 0) {
			framestart = fileInfo.nImages - framenumber + 1;
		}

		fileInfo.offset += fileInfo.width * datatype * fileInfo.height * (framestart - 1);
		oldnImages = fileInfo.nImages;
		fileInfo.nImages = framenumber;

		// Leave the job to ImageJ for actually reading the image
                FileOpener fo = new FileOpener(fileInfo);
                ImagePlus imp = fo.open(false);
                if(fileInfo.nImages==1) {
                  ImageProcessor ip = imp.getProcessor();              
                  // ip.flipVertical(); // origin is at bottom left corner
                  setProcessor(fileName, ip);
                } else {
                  ImageStack stack = imp.getStack(); 
        	  // origin is at bottom left corner              
                  // for(int i=1; i<=stack.getSize(); i++)
                  //   stack.getProcessor(i).flipVertical();
                  setStack(fileName, stack);
                }
                Calibration cal = imp.getCalibration();
                //if (fileInfo.fileType==FileInfo.GRAY16_SIGNED && fd.bscale==1.0 && fd.bzero==32768.0)
                //    cal.setFunction(Calibration.NONE, null, "Gray Value");
                setCalibration(cal);
                //  setProperty("Info", fd.getHeaderInfo());
                setFileInfo(fileInfo); // needed for File->Revert

		IJ.run(imp, "Enhance Contrast", "saturated=0.55");

                if (arg.equals("")) show();

            while (update) {

		try	{

				Thread.sleep(500);
		      	}

		catch(InterruptedException e) { }

		datatype = parseBAMDataFormatHeader(type, f, fileInfo);

		if (fileInfo.nImages != oldnImages) {

			framestart = fileInfo.nImages;
			fileInfo.offset += fileInfo.width * datatype * fileInfo.height * (framestart - 1);
			oldnImages = fileInfo.nImages;
			fileInfo.nImages = 1;

                	FileOpener fo2 = new FileOpener(fileInfo);
			ImagePlus imp2 = fo2.open(false);
                  	setProcessor(fileName, imp2.getProcessor());
                	Calibration cal2 = imp2.getCalibration();
                	setCalibration(cal2);
                	setFileInfo(fileInfo); // needed for File->Revert

			IJ.run(imp2, "Enhance Contrast", "saturated=0.35");
			IJ.run(imp2, "Brightness/Contrast...", "");

			show();

		}

            }
        
      	}

	private int parseBAMDataFormatHeader(String type, File f, FileInfo fileInfo) {

		RandomAccessFile in;
		String headerfilename;
		int datatype;

		try {
			in = new RandomAccessFile(f, "r");
			headerfilename = getFilename(in);
			if (headerfilename.charAt(11) == 'x') {
                                fileInfo.intelByteOrder = false;
                        }
			else {
			        fileInfo.intelByteOrder = true;
                        }
			if (headerfilename.charAt(8) == 'd') {
				getLong(in, fileInfo.intelByteOrder); // 1
				fileInfo.width = getLong(in, fileInfo.intelByteOrder); // 2
				fileInfo.nImages = getLong(in, fileInfo.intelByteOrder); // 3
				getLong(in, fileInfo.intelByteOrder); // 4
				fileInfo.height = getLong(in, fileInfo.intelByteOrder); // 5
			}
			else {
				fileInfo.width = getLong(in, fileInfo.intelByteOrder); // 1
				fileInfo.height = getLong(in, fileInfo.intelByteOrder); // 2
				getLong(in, fileInfo.intelByteOrder); // 3
				getLong(in, fileInfo.intelByteOrder); // 4
				fileInfo.nImages = getLong(in, fileInfo.intelByteOrder); // 5
			}
			getLong(in, fileInfo.intelByteOrder); // 6
			getLong(in, fileInfo.intelByteOrder); // 7
			getLong(in, fileInfo.intelByteOrder); // 8
			getLong(in, fileInfo.intelByteOrder); // 9
			datatype = getLong(in, fileInfo.intelByteOrder); // 10
                        if (datatype == 1) {
					fileInfo.fileType = fileInfo.GRAY8;
                        }
			else if (datatype == 2) {
					fileInfo.fileType = fileInfo.GRAY16_UNSIGNED;
                        }
			else if (datatype == 4) {
					fileInfo.fileType = fileInfo.GRAY32_FLOAT;
                        }
			else {
					IJ.showMessage("WARNING: unknown data type " + Integer.toString(datatype));
                        }

			fileInfo.offset = fileInfo.width * datatype;
			while (fileInfo.offset < 512) {
				fileInfo.offset += fileInfo.width * datatype;
                        }
		} catch (IOException ex) {
			IJ.showMessage("IOException caught: " + ex);
			return 0;
		}

		if (IJ.debugMode)
			IJ.log("ImportDialog: " + fileInfo);

		return datatype;
	}

	String getFilename(RandomAccessFile in) throws IOException {
		String filename = "";

                for (int i = 0; i < 12; i++)
			filename += getChar(in);
		return filename;
        }

	int getShort(RandomAccessFile in, boolean intelByteOrder) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if (intelByteOrder)
			return ((b2 << 8) + b1);
		else
			return ((b1 << 8) + b2);
	}

	int getLong(RandomAccessFile in, boolean intelByteOrder) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		if (intelByteOrder)
			return ((b4 << 24) + (b3 << 16) + (b2 << 8) + b1);
		else
			return ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
	}

	char getChar(RandomAccessFile in) throws IOException {
		int b = in.read();
		return (char) b;
	}

	void showAbout() {
		String message = "This plugin reads BAM-CT image formats.\n"
				+ "It can currently read the following formats:\n \n";
		for (int i = 0; i < types.length; i++) {
			message += types[i] + ":   " + typesDescription[i] + "\n";
		}
		message += " \n" + "This plugin is written and maintained by BAM-CT-Group.\n"
				+ "This is version " + BAMRead_VERSION + "\n";
		IJ.showMessage("About BAM Reader...", message);
	}
}

// eof BAMRead_.java
