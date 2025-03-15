package lab.SECRET;

import java.util.Scanner;

public class ROT42069 {
    private int letterShift = 15;
    private int digitShift = 9;
    private int symbolShift = 69;
    public String rot42069(String text) {
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                c = (char) ((c - base + 15) % 26 + base);
            } else if (Character.isDigit(c)) {
                char base = '0';
                c = (char) ((c - base + 9) % 10 + base);
            } else if (c >= 32 && c <= 126) {
                c = (char) ((c - 32 + 69) % 95 + 32);
            }
            result.append(c);
        }

        return result.toString();
    }
    public String rot69420(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                c = (char) ((c - base + 11) % 26 + base);
            } else if (Character.isDigit(c)) {
                char base = '0';
                c = (char) ((c - base + 1) % 10 + base);
            } else if (c >= 32 && c <= 126) {
                c = (char) ((c - 32 + 26) % 95 + 32);
            }
            result.append(c);
        }

        return result.toString();
    }

    public static void main(String[] args) {
        ROT42069 obj = new ROT42069();
        System.out.println(obj.rot69420("012"));
    }
}

