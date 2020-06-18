package temp;

import java.io.IOException;

/**
 * User : rahul
 * Date : 09/12/19
 * Time : 12:16 PM
 */
public class Master {
    public static void main(String[] args) throws IOException {
        final String operation = args[0];
        if (operation.equals("encode")) {
            final String toHideInto = args[1]; //"/Users/rahul/Pictures/far-cry-5-ps4-wallpaper-01.jpeg"
            final String toHide = args[2];//"/Users/rahul/Pictures/horizon-zero-dawn-ps4.jpeg"
            final String toSavePath = args[3]; //"/Users/rahul/Pictures/temp2.png"
            ImageSteganographer.encode(toHideInto, toHide, toSavePath);
            System.out.println("success");
        } else {
            final String sourceImage = args[1];
            final String targetDirectory = args[2];
            ImageSteganographer.decode(sourceImage, targetDirectory);
            System.out.println("success");
        }
    }
}
