import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class gg {
    private static final int bitsInByte = 8;

    static String originalImageFile;
    static int chunkSize = 1; // byte
    static BufferedImage redSecret;
    static BufferedImage greenSecret;
    static BufferedImage blueSecret;
    static  BufferedImage imageRed;
    static BufferedImage imageGreen;
    static  BufferedImage imageBlue;


    public static void main(String[] args) {
        originalImageFile = "C:\\Users\\Lenovo\\Desktop\\sample_1920×1280.bmp";
        encode(originalImageFile, "C:\\Users\\Lenovo\\Desktop\\Encryption\\msg.txt", 1);


    }

    public static void encode(String imagePath, String textFilePath, int chunkSize) {

        try {
            BufferedImage originalImage = ImageIO.read(new File(originalImageFile));


            // Create three separate BufferedImage objects for red, green, and blue sub-images
            imageRed = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            imageGreen = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            imageBlue = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < originalImage.getWidth(); x++) {
                for (int y = 0; y < originalImage.getHeight(); y++) {
                    int pixel = originalImage.getRGB(x, y);
                    int red = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = pixel & 0xFF;

                    int maxColor = Math.max(Math.max(red, green), blue);

                    // Decide which sub-image to place the pixel in based on the greatest color component
                    if (maxColor == red) {
                        imageRed.setRGB(x, y, pixel);

                    } else if (maxColor == green) {
                        imageGreen.setRGB(x, y, pixel);

                    } else {
                        imageBlue.setRGB(x, y, pixel);

                    }
                }
            }
            BufferedImage rejoinedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);

            // Copy pixel data from the sub-images to the corresponding regions in the rejoinedImage


            // Save the sub-images
            ImageIO.write(imageRed, "PNG", new File("image_red.png"));
            ImageIO.write(imageGreen, "PNG", new File("image_green.png"));
            ImageIO.write(imageBlue, "PNG", new File("image_blue.png"));


            System.out.println("Images saved successfully.");


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(textFilePath))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            // Read the file line by line and append it to the StringBuilder
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            // Now you have the entire content of the file in the StringBuilder
            String fileContent = stringBuilder.toString();
            System.out.println("file content " + fileContent);
            byte[] greenImageBytes = getBytesFromImage(imageGreen);
            byte[] blueImageBytes = getBytesFromImage(imageBlue);
            BufferedImage imageInUserSpace = getImageInUserSpace(imageRed);
            byte[] textBytes = fileContent.getBytes();

            byte[] ImageRedBytes = getBytesFromImage(imageInUserSpace);

            int imagePointer = 0;

            for (int i = 0; i < textBytes.length; i += 3) {
                for (int j = 0; j < 8; j++) {
                    if (imagePointer < ImageRedBytes.length) {
                        // Clear the least significant bit of the red byte
                        ImageRedBytes[imagePointer] &= 0xFE; //AND 11111110

                        // Get the j-th bit of the text byte and set it as the LSB of the red byte
                        int bit = (textBytes[i] >> j) & 1;
                        ImageRedBytes[imagePointer] |= bit;
                    }
                    imagePointer++;
                }
            }




        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static BufferedImage getImageInUserSpace(BufferedImage image) {
        BufferedImage imageInUserSpace  = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = imageInUserSpace.createGraphics(); //The Graphics2D class provides a way to draw and manipulate graphics within the image.
        graphics.drawRenderedImage(image, null);//graphics.drawRenderedImage(image, null): This line draws the content of the original input image onto the imageInUserSpace.
        // Essentially, it copies the pixels from the original image to the new image.
        graphics.dispose();
        return imageInUserSpace;
    }


    private static String decode(BufferedImage image) {
        byte[] decodedHiddenText;
        try {
            BufferedImage imageInUserSpace = getImageInUserSpace(image);
            byte imageInBytes[] = getBytesFromImage(imageInUserSpace);
            decodedHiddenText = decodeImage(imageInBytes);
            String hiddenText = new String(decodedHiddenText, StandardCharsets.US_ASCII); // Use "UTF-8" encoding
            String outputFileName = "hidden_text.txt";
            saveTextToPath(hiddenText, new File(outputFileName));
            System.out.println("Successfully extracted text to: " + outputFileName);
            return hiddenText;
        } catch (Exception exception) {
            System.out.println("No hidden message. Error: " + exception);
            return "";
        }
    }


    private static byte[] decodeImage(byte[] image) {
        int length = 15; // Adjust the expected text length accordingly
        int offset = 0;

        byte[] result = new byte[length];

        for (int b = 0; b < length; b++) {
            for (int i = 0; i < 8; i++, offset++) { // Use 8 bits for each character
                result[b] = (byte) ((result[b] << 1) | (image[offset] & 0x1));
            }
        }
        return result;
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





    public static byte[] getBytesFromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                // Extract the RGB components
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // Add the RGB components to the byte stream
                byteStream.write(red);
                byteStream.write(green);
                byteStream.write(blue);
            }
        }

        return byteStream.toByteArray();
    }

}
