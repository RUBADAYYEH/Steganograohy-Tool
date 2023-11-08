import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBufferByte;


public class Steganographer {
    private static int bytesForTextLengthData = 4; //size of an int .
    private static int bitsInByte = 8;


    static  BufferedImage originalImage;

  static   byte redChannelBytes[];
   static byte greenChannelBytes[];
   static  byte blueChannelBytes[];


    public static void main(String[] args) {

        if (args.length > 0) {
            if (args.length == 1) {
                if (args[0].equals("--help")) {
                    System.out.println("");
                    System.out.println("-- STEGANOGRAPHER --");
                    System.out.println("");
                    System.out.println("Hide or reveal data in images!");
                    System.out.println("");
                    System.out.println("For encode mode provide two arguments as specified below:");
                    System.out.println("java Steganographer <path_to_container_image> <path_to_message_text_file>");
                    System.out.println("");
                    System.out.println("For decode mode provide only one argument as specified below:");
                    System.out.println("java Steganographer <path_to_image_with_hidden_message>");
                    System.out.println("");
                    return;
                } else {
                    decode(args[0]);
                    return;
                }
            } else if (args.length == 2) {
                try {

                    encode(args[0], args[1]);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
        System.out.println("Wrong input. Use '--help' option for more information.");
    }

    public static int hash(String str) {
        int hash = 11; // Initialize the hash with a prime number, which helps with distribution

        // Iterate through each character in the input stringp
        for (int i = 0; i < str.length(); i++) {
            // Left-shift the current hash value by 5 bits and add the current character's Unicode value
            hash = ((hash << 5) + hash) + str.charAt(i);
        }

        return hash; // Return the final hash value
    }


    // Encode

    public static void encode(String imagePath, String textPath) throws IOException {

            originalImage = ImageIO.read(new File(imagePath));

            BufferedImage imageInUserSpace = getImageInUserSpace(originalImage);
            String text = getTextFromTextFile(textPath);

               int hash=hash(text);


            byte[] imageInBytes = getBytesFromImage(imageInUserSpace);
            byte[] textInBytes = text.getBytes();// array of bytes used in a strting

             byte[] textLengthInBytes = getBytesFromInt(textInBytes.length);
        byte[] hashInBytes = getBytesFromInt(hash);

             //distribute bytes over channels equally .

            redChannelBytes= new byte[(textInBytes.length / 3 )+ (textInBytes.length % 3)];
            int redcount = 0;

            greenChannelBytes = new byte[textInBytes.length / 3];
            int greencout = 0;

             blueChannelBytes = new byte[textInBytes.length / 3];
            int bluecount = 0;

            for (int i = 0; i < textInBytes.length; i += 3) {
                if (textInBytes.length-i==2){
                    redChannelBytes[redcount++]=textInBytes[i];
                    i++;
                    redChannelBytes[redcount++]=textInBytes[i++];
                } else if (textInBytes.length-i==1) {
                    redChannelBytes[redcount++]=textInBytes[i++];
                }
                if (redcount < redChannelBytes.length) {
                    redChannelBytes[redcount++] = textInBytes[i];
                }

                if (greencout < greenChannelBytes.length) {
                    greenChannelBytes[greencout++] = textInBytes[i + 1];
                }

                if (bluecount < blueChannelBytes.length) {
                    blueChannelBytes[bluecount++] = textInBytes[i + 2];
                }



            }
       // System.out.println("green channel bytes : "+ Arrays.toString(greenChannelBytes));
       // System.out.println("red Channel Bytes :"+Arrays.toString(redChannelBytes));
       // System.out.println("blue Channel Bytes :"+Arrays.toString(blueChannelBytes));




            try {

               encodeImage(imageInBytes, textLengthInBytes,  0,hashInBytes); // to embed the message  length


                encodeImage2(imageInBytes, redChannelBytes,greenChannelBytes,blueChannelBytes,(bytesForTextLengthData*bitsInByte)+(4*bitsInByte)); ///embed the actual message


            } catch (Exception exception) {
                System.out.println("Couldn't hide text in image. Error: " + exception);
                return;
            }





            System.out.println("Successfully encoded text in: " + "original_with_hidden_message.png");

            saveImageToPath(imageInUserSpace, new File("original_with_hidden_message.png"), "png");





         }





    //to encode text length 4 byte .
    private static byte[] encodeImage(byte[] image, byte[] addition, int offset,byte[] hash) {
        if (addition.length + offset > image.length) {
            throw new IllegalArgumentException("Image file is not long enough to store provided text");
        }
        for (int i=0; i<addition.length; i++) {
            int additionByte = addition[i]; //  (loops)takes every byte of  the text .
            for (int bit=bitsInByte-1; bit>=0; --bit, offset++) {  // loops through every bit of a bite .
                int b = (additionByte >>> bit) & 0x1;                 //0x1: This is a hexadecimal representation of the binary value 00000001.
                // It's used to mask the result of the right shift operation to
                //                                                    ensure that only the least significant bit remains.
                image[offset] = (byte)((image[offset] & 0xFE) | b); //clear the lest significant bit in the image byte and or it with b .
            }
        }
        for (int i=0;i<hash.length;i++){
            int hashByte=hash[i];
            for (int bit=bitsInByte-1; bit>=0; --bit, offset++) {
                int b = (hashByte >>> bit) & 0x1;                 //0x1: This is a hexadecimal representation of the binary value 00000001.
                // It's used to mask the result of the right shift operation to
                //                                                    ensure that only the least significant bit remains.
                image[offset] = (byte)((image[offset] & 0xFE) | b); //clear the lest significant bit in the image byte and or it with b .
            }

        }

        return image;
    }

    //encode actual message
    public static byte[] encodeImage2(byte[] image, byte[] redChannelBytes, byte[] greenChannelBytes, byte[] blueChannelBytes,int offset) {
        System.out.println("red channel bytes length is "+redChannelBytes.length);
        System.out.println("green channel bytes length is "+greenChannelBytes.length);
        System.out.println("blue channel bytes length is "+blueChannelBytes.length);

        boolean[] redbitArray = new boolean[redChannelBytes.length * 8];
        int bitIndex = 0;

        for (int i = 0; i < redChannelBytes.length; i++) {
            byte b = redChannelBytes[i];
            for (int j = 7; j >= 0; j--) {
                redbitArray[bitIndex++] = ((b >> j) & 1) == 1;
            }
        }

        boolean[] greenbitArray = new boolean[greenChannelBytes.length * 8];
        int greenbitIndex = 0;

        for (int i = 0; i < greenChannelBytes.length; i++) {
            byte b = greenChannelBytes[i];
            for (int j = 7; j >= 0; j--) {
                greenbitArray[greenbitIndex++] = ((b >> j) & 1) == 1;
            }
        }

        boolean[] bluebitArray = new boolean[blueChannelBytes.length * 8];
        int bluebitIndex = 0;

        for (int i = 0; i < blueChannelBytes.length; i++) {
            byte b = blueChannelBytes[i];
            for (int j = 7; j >= 0; j--) {
                bluebitArray[bluebitIndex++] = ((b >> j) & 1) == 1;
            }
        }


        int redcount = 0;
        int greencount = 0;
        int bluecount = 0;

        System.out.println("length of red bit Array "+redbitArray.length);
        System.out.println("length of green bit Array "+greenbitArray.length);
        System.out.println("length of blue bit Array "+bluebitArray.length);
        System.out.println("size of image "+image.length);


        while ((redcount<redbitArray.length||bluecount<bluebitArray.length||greencount<greenbitArray.length) && offset<image.length){

            byte redChannel = image[offset];
            byte greenChannel = image[offset+1];
            byte blueChannel = image[offset+2];


            byte maxChannel = (byte) Math.max(redChannel, Math.max(greenChannel, blueChannel));

            if (maxChannel != 0) {
                offset=offset+3;

                    if (maxChannel == redChannel && redcount < redbitArray.length) {

                        if (redcount == redbitArray.length - 1) {
                            int b = redbitArray[redcount++] ? 1 : 0;

                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset += 3;
                        } else if (redcount == redbitArray.length - 2) {
                            int b = redbitArray[redcount++] ? 1 : 0;
                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset++;
                            b = redbitArray[redcount++] ? 1 : 0;
                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset += 2;
                        } else {
                            for (int i = 0; i < 3; i++, offset++) { // loops through every bit of a bite .
                                int b = redbitArray[redcount++] ? 1 : 0;          //0x1: This is a hexadecimal representation of the binary value 00000001.
                                // It's used to mask the result of the right shift operation to
                                //                                                    ensure that only the least significant bit remains.
                                image[offset] = (byte) ((image[offset] & 0xFE) | b); //clear the lest significant bit in the image byte and or it with b .
                            }

                        }
                    } else if (maxChannel == greenChannel && greencount < greenbitArray.length) {

                        if (greencount == greenbitArray.length - 1) {
                            int b = greenbitArray[greencount++] ? 1 : 0;
                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset += 3;
                        } else if (greencount == greenbitArray.length - 2) {
                            int b = greenbitArray[greencount++] ? 1 : 0;
                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset++;
                            b = greenbitArray[greencount++] ? 1 : 0;
                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset += 2;
                        } else {
                            for (int i = 0; i < 3; i++, offset++) {  // loops through every bit of a bite .
                                int b = greenbitArray[greencount++] ? 1 : 0;          //0x1: This is a hexadecimal representation of the binary value 00000001.
                                // It's used to mask the result of the right shift operation to
                                //                                                    ensure that only the least significant bit remains.
                                image[offset] = (byte) ((image[offset] & 0xFE) | b); //clear the lest significant bit in the image byte and or it with b .
                            }

                        }
                    } else if (maxChannel == blueChannel && bluecount < bluebitArray.length) {

                        if (bluecount == bluebitArray.length - 1) {
                            int b = bluebitArray[bluecount++] ? 1 : 0;
                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset += 3;
                        } else if (bluecount == bluebitArray.length - 2) {
                            int b = bluebitArray[bluecount++] ? 1 : 0;
                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset++;
                            b = bluebitArray[bluecount++] ? 1 : 0;
                            image[offset] = (byte) ((image[offset] & 0xFE) | b);
                            offset += 2;
                        } else {
                            for (int i = 0; i < 3; i++, offset++) {  // loops through every bit of a bite .
                                int b = bluebitArray[bluecount++] ? 1 : 0;          //0x1: This is a hexadecimal representation of the binary value 00000001.
                                // It's used to mask the result of the right shift operation to
                                //                                                    ensure that only the least significant bit remains.
                                image[offset] = (byte) ((image[offset] & 0xFE) | b); //clear the lest significant bit in the image byte and or it with b .
                            }

                        }
                    } else if ((maxChannel == redChannel && redcount == redbitArray.length) || (maxChannel == greenChannel && greencount == greenbitArray.length) || (maxChannel == blueChannel && bluecount == bluebitArray.length)) {
                        offset += 3;
                    }
                } else {
                    offset += 6;
                }


        }


        return image;
    }


    // Decode

    public static String decode(String imagePath) {
        byte[] decodedHiddenText;
        try {
            BufferedImage imageFromPath = getImageFromPath(imagePath);
            BufferedImage imageInUserSpace = getImageInUserSpace(imageFromPath);
            byte imageInBytes[] = getBytesFromImage(imageInUserSpace);


            decodedHiddenText = decodeImage(imageInBytes);
            String hiddenText = new String(decodedHiddenText);



            String outputFileName = imagePath+"hidden_text.txt";
            saveTextToPath(hiddenText, new File(outputFileName));
            System.out.println("Successfully extracted text to: " + outputFileName);
            return hiddenText;
        } catch (Exception exception) {
            System.out.println("No hidden message. Error: " + exception);
            return "";
        }
    }
 //to decode the length of the text
    public static byte[] decodeImage(byte[] image) {
        int length = 0;
        int offset  = bytesForTextLengthData*bitsInByte;
        int hashOffset=8*8;
        int hash=0;

        for (int i=0; i<offset; i++) {    // to calculate the length of the hidden message .
           // System.out.println(image[i]);
            length = (length << 1) | (image[i] & 0x1); //left shift and extract the lsb.
        }for (int i=offset; i<hashOffset; i++) {    // to calculate the length of the hidden message .
            // System.out.println(image[i]);
           hash = (hash << 1) | (image[i] & 0x1); //left shift and extract the lsb.
        }


       System.out.println("length of text is "+length);
        System.out.println("hash is "+hash);

        byte[] result = new byte[length];

       boolean[] redbitArray=new boolean[((length/3)+(length%3))*8];
        boolean[] greenbitArray=new boolean[(length/3)*8];
        boolean[] bluebitArray=new boolean[(length/3)*8];
           //to keep track of the array indeces .
        int redcount=0;
        int greencount=0;
        int bluecount=0;

        System.out.println("red bit length "+redbitArray.length);
        System.out.println("green bit legnth "+ greenbitArray.length);
        System.out.println("blue bit array length" +bluebitArray.length);
        offset=hashOffset;

        while ((redcount<redbitArray.length||bluecount<bluebitArray.length||greencount<greenbitArray.length) && offset<image.length) {


            byte redChannel = image[offset];
            byte greenChannel = image[offset + 1];
            byte blueChannel = image[offset + 2];




            byte maxChannel = (byte) Math.max(redChannel, Math.max(greenChannel, blueChannel));

            if (maxChannel != 0) {
                offset=offset+3;

                    if (maxChannel == redChannel && redcount < redbitArray.length) {

                        if (redcount == redbitArray.length - 1) {
                            redbitArray[redcount++] = (((image[offset] & 0x1) == 1));
                            offset += 3;
                        } else if (redcount == redbitArray.length - 2) {
                            redbitArray[redcount++] = (((image[offset] & 0x1) == 1));
                             offset++;
                            redbitArray[redcount++] = (((image[offset] & 0x1) == 1));
                            offset += 2;
                        } else {
                            for (int i = 0; i < 3; i++, offset++) {
                                redbitArray[redcount++] = (((image[offset] & 0x1) == 1));

                            }

                        }
                    } else if (maxChannel == greenChannel && greencount < greenbitArray.length) {

                        if (greencount == greenbitArray.length - 1) {
                            greenbitArray[greencount++] = (((image[offset] & 0x1) == 1));

                            offset += 3;
                        } else if (greencount == greenbitArray.length - 2) {
                            greenbitArray[greencount++] = (((image[offset] & 0x1) == 1));
                            offset++;
                            greenbitArray[greencount++] = (((image[offset] & 0x1) == 1));
                            offset += 2;
                        } else {
                            for (int i = 0; i < 3; i++, offset++) {
                                greenbitArray[greencount++] = (((image[offset] & 0x1) == 1));

                            }

                        }
                    } else if (maxChannel == blueChannel && bluecount < bluebitArray.length) {

                        if (bluecount == bluebitArray.length - 1) {
                            bluebitArray[bluecount++] = (((image[offset] & 0x1) == 1));
                            offset += 3;

                        } else if (bluecount == bluebitArray.length - 2) {
                            bluebitArray[bluecount++] = (((image[offset] & 0x1) == 1));
                            offset++;
                            bluebitArray[bluecount++] = (((image[offset] & 0x1) == 1));
                            offset += 2;
                        } else {
                            for (int i = 0; i < 3; i++, offset++) {
                                bluebitArray[bluecount++] = (((image[offset] & 0x1) == 1));

                            }

                        }
                    } else if ((maxChannel == redChannel && redcount == redbitArray.length) || (maxChannel == greenChannel && greencount == greenbitArray.length) || (maxChannel == blueChannel && bluecount == bluebitArray.length)) {
                        offset += 3;
                    }
                } else {
                    offset += 6;

                }



        }

       // System.out.println("next offset "+image[offset]+ ", "+image[offset+1]+","+image[offset+2]);



        int resultIndex = 0;
        int redIndex = 0;
        int greenIndex = 0;
        int blueIndex = 0;


        while ((redIndex <redbitArray.length || greenIndex < greenbitArray.length || blueIndex < bluebitArray.length)&&resultIndex<result.length) {


            // Form an 8-bit sequence from redBits
            byte redByte = 0;
            for (int i = 0; i < 8; i++) {
                if (redIndex < redbitArray.length) {
                    boolean redBit = redbitArray[redIndex];
                    redByte = (byte) (redByte << 1 | (redBit ? 1 : 0));
                    redIndex++;
                } else {
                    // Pad with zeros if there are fewer than 8 bits left
                    redByte = (byte) (redByte << 1);
                }
            }

            // Form an 8-bit sequence from greenBits
            byte greenByte = 0;
            for (int i = 0; i < 8; i++) {
                if (greenIndex < greenbitArray.length ) {
                    boolean greenBit = greenbitArray[greenIndex];
                    greenByte = (byte) (greenByte << 1 | (greenBit ? 1 : 0));
                    greenIndex++;
                } else {
                    // Pad with zeros if there are fewer than 8 bits left
                    greenByte = (byte) (greenByte << 1);
                }
            }

            // Form an 8-bit sequence from blueBits
            byte blueByte = 0;
            for (int i = 0; i < 8; i++) {
                if (blueIndex < bluebitArray.length) {
                    boolean blueBit = bluebitArray[blueIndex];
                    blueByte = (byte) (blueByte << 1 | (blueBit ? 1 : 0));
                    blueIndex++;
                } else {
                    // Pad with zeros if there are fewer than 8 bits left
                    blueByte = (byte) (blueByte << 1);
                }
            }

            // Save the 8-bit sequences into the result array
            if (resultIndex==result.length-1){
                result[resultIndex++] = redByte;
                System.out.println(redByte);
            } else if (resultIndex==result.length-2) {
                result[resultIndex++] = redByte;
                System.out.println(redByte);
                 redByte = 0;
                for (int i = 0; i < 8; i++) {
                    if (redIndex < redbitArray.length) {
                        boolean redBit = redbitArray[redIndex];
                        redByte = (byte) (redByte << 1 | (redBit ? 1 : 0));
                        redIndex++;
                    } else {
                        // Pad with zeros if there are fewer than 8 bits left
                        redByte = (byte) (redByte << 1);
                    }
                }
                System.out.println(redByte);
                result[resultIndex++] = redByte;
            }
            else{
            result[resultIndex] = redByte;
            System.out.println("red byte "+redByte);
            result[resultIndex + 1] = greenByte;
            System.out.println(greenByte);
            result[resultIndex + 2] = blueByte;
            resultIndex += 3;
            System.out.println(blueByte);
            }


        }
        String hiddenText = new String(result);
        int hashResult=hash(hiddenText);
        if (hashResult==hash){
            System.out.println("its a true message . verified hash is "+hash);
        }
        else{
            System.out.println("its a false message . verified hash is "+hash);
        }


        return result;

    }
    public static byte[] booleanArrayToByteArray(boolean[] boolArray) {
        byte[] byteArray = new byte[boolArray.length / 8];

        for (int i = 0; i < boolArray.length / 8; i++) {
            for (int j = 0; j < 8; j++) {
                byteArray[i] |= (byte) ((boolArray[i * 8 + j] ? 1 : 0) << (7 - j));
            }
        }

        return byteArray;
    }

    public static void printAsciiValues(byte[] byteArray) {
        for (byte b : byteArray) {
            System.out.print((char) b);
        }
        System.out.println();
    }







    private static void saveImageToPath(BufferedImage image, File file, String extension) {
        try {
            file.delete();
            ImageIO.write(image, extension, file);
        } catch (Exception exception) {
            System.out.println("Image file could not be saved. Error: " + exception);
        }
    }

    private static void saveTextToPath(String text, File file) {
        try {
            if (file.exists() == false) {
                file.createNewFile( );
            }
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(text);
            bufferedWriter.close();
        } catch (Exception exception) {
            System.out.println("Couldn't write text to file: " + exception);
        }
    }

    private static BufferedImage getImageFromPath(String path) {
        BufferedImage image	= null;
        File file = new File(path);
        try {
            image = ImageIO.read(file);
        } catch (Exception exception) {
            System.out.println("Input image cannot be read. Error: " + exception);
        }
        return image;
    }

    private static String getTextFromTextFile(String textFile) {
        String text = "";
        try {
            Scanner scanner = new Scanner( new File(textFile) );
            text = scanner.useDelimiter("\\A").next();   // scanner.useDelimiter("\\A") sets the delimiter for the scanner to \\A,
            // which is a regular expression pattern that matches the beginning of input.
            // Essentially, it instructs the scanner to read the entire contents of the file as a single token.

            scanner.close();
        } catch (Exception exception) {
            System.out.println("Couldn't read text from file. Error: " + exception);
        }
        return text;
    }


    // Helpers

    //creating a copy of image . in type model of rgb to make sure it doesnt affect the original image .

    private static BufferedImage getImageInUserSpace(BufferedImage image) {
        BufferedImage imageInUserSpace  = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = imageInUserSpace.createGraphics(); //The Graphics2D class provides a way to draw and manipulate graphics within the image.
        graphics.drawRenderedImage(image, null);//graphics.drawRenderedImage(image, null): This line draws the content of the original input image onto the imageInUserSpace.
        // Essentially, it copies the pixels from the original image to the new image.
        graphics.dispose();
        return imageInUserSpace;
    }

    private static byte[] getBytesFromImage(BufferedImage image) {
        WritableRaster raster = image.getRaster(); //The raster represents the rectangular array of pixels that make up the image.
        DataBufferByte buffer = (DataBufferByte)raster.getDataBuffer(); // convert the pixels into bytes of rgb .
        return buffer.getData();
    }

    private static byte[] getBytesFromInt(int integer) {

        return ByteBuffer.allocate(bytesForTextLengthData).putInt(integer).array();
    }


    }