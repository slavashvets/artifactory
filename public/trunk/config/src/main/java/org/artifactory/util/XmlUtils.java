/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.util;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class XmlUtils {

    private static final byte[] FIRST_NAME_PAGES =
            {
                    0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x00,
                    0x00, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
                    0x10, 0x11, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x12, 0x13,
                    0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x15, 0x16, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x17,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x18,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
            };
    private static final byte[] NAME_PAGES =
            {
                    0x19, 0x03, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x00,
                    0x00, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25,
                    0x10, 0x11, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x12, 0x13,
                    0x26, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x27, 0x16, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x17,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x18,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
            };
    private static final int[] NAME_BITMAP =
            {
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
                    0x00000000, 0x04000000, 0x87FFFFFE, 0x07FFFFFE,
                    0x00000000, 0x00000000, 0xFF7FFFFF, 0xFF7FFFFF,
                    0xFFFFFFFF, 0x7FF3FFFF, 0xFFFFFDFE, 0x7FFFFFFF,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFE00F, 0xFC31FFFF,
                    0x00FFFFFF, 0x00000000, 0xFFFF0000, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xF80001FF, 0x00000003, 0x00000000,
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0xFFFFD740, 0xFFFFFFFB, 0x547F7FFF, 0x000FFFFD,
                    0xFFFFDFFE, 0xFFFFFFFF, 0xDFFEFFFF, 0xFFFFFFFF,
                    0xFFFF0003, 0xFFFFFFFF, 0xFFFF199F, 0x033FCFFF,
                    0x00000000, 0xFFFE0000, 0x027FFFFF, 0xFFFFFFFE,
                    0x0000007F, 0x00000000, 0xFFFF0000, 0x000707FF,
                    0x00000000, 0x07FFFFFE, 0x000007FE, 0xFFFE0000,
                    0xFFFFFFFF, 0x7CFFFFFF, 0x002F7FFF, 0x00000060,
                    0xFFFFFFE0, 0x23FFFFFF, 0xFF000000, 0x00000003,
                    0xFFF99FE0, 0x03C5FDFF, 0xB0000000, 0x00030003,
                    0xFFF987E0, 0x036DFDFF, 0x5E000000, 0x001C0000,
                    0xFFFBAFE0, 0x23EDFDFF, 0x00000000, 0x00000001,
                    0xFFF99FE0, 0x23CDFDFF, 0xB0000000, 0x00000003,
                    0xD63DC7E0, 0x03BFC718, 0x00000000, 0x00000000,
                    0xFFFDDFE0, 0x03EFFDFF, 0x00000000, 0x00000003,
                    0xFFFDDFE0, 0x03EFFDFF, 0x40000000, 0x00000003,
                    0xFFFDDFE0, 0x03FFFDFF, 0x00000000, 0x00000003,
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0xFFFFFFFE, 0x000D7FFF, 0x0000003F, 0x00000000,
                    0xFEF02596, 0x200D6CAE, 0x0000001F, 0x00000000,
                    0x00000000, 0x00000000, 0xFFFFFEFF, 0x000003FF,
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0x00000000, 0xFFFFFFFF, 0xFFFF003F, 0x007FFFFF,
                    0x0007DAED, 0x50000000, 0x82315001, 0x002C62AB,
                    0x40000000, 0xF580C900, 0x00000007, 0x02010800,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
                    0x0FFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0x03FFFFFF,
                    0x3F3FFFFF, 0xFFFFFFFF, 0xAAFF3F3F, 0x3FFFFFFF,
                    0xFFFFFFFF, 0x5FDFFFFF, 0x0FCF1FDC, 0x1FDC1FFF,
                    0x00000000, 0x00004C40, 0x00000000, 0x00000000,
                    0x00000007, 0x00000000, 0x00000000, 0x00000000,
                    0x00000080, 0x000003FE, 0xFFFFFFFE, 0xFFFFFFFF,
                    0x001FFFFF, 0xFFFFFFFE, 0xFFFFFFFF, 0x07FFFFFF,
                    0xFFFFFFE0, 0x00001FFF, 0x00000000, 0x00000000,
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
                    0xFFFFFFFF, 0x0000003F, 0x00000000, 0x00000000,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
                    0xFFFFFFFF, 0x0000000F, 0x00000000, 0x00000000,
                    0x00000000, 0x07FF6000, 0x87FFFFFE, 0x07FFFFFE,
                    0x00000000, 0x00800000, 0xFF7FFFFF, 0xFF7FFFFF,
                    0x00FFFFFF, 0x00000000, 0xFFFF0000, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xF80001FF, 0x00030003, 0x00000000,
                    0xFFFFFFFF, 0xFFFFFFFF, 0x0000003F, 0x00000003,
                    0xFFFFD7C0, 0xFFFFFFFB, 0x547F7FFF, 0x000FFFFD,
                    0xFFFFDFFE, 0xFFFFFFFF, 0xDFFEFFFF, 0xFFFFFFFF,
                    0xFFFF007B, 0xFFFFFFFF, 0xFFFF199F, 0x033FCFFF,
                    0x00000000, 0xFFFE0000, 0x027FFFFF, 0xFFFFFFFE,
                    0xFFFE007F, 0xBBFFFFFB, 0xFFFF0016, 0x000707FF,
                    0x00000000, 0x07FFFFFE, 0x0007FFFF, 0xFFFF03FF,
                    0xFFFFFFFF, 0x7CFFFFFF, 0xFFEF7FFF, 0x03FF3DFF,
                    0xFFFFFFEE, 0xF3FFFFFF, 0xFF1E3FFF, 0x0000FFCF,
                    0xFFF99FEE, 0xD3C5FDFF, 0xB080399F, 0x0003FFCF,
                    0xFFF987E4, 0xD36DFDFF, 0x5E003987, 0x001FFFC0,
                    0xFFFBAFEE, 0xF3EDFDFF, 0x00003BBF, 0x0000FFC1,
                    0xFFF99FEE, 0xF3CDFDFF, 0xB0C0398F, 0x0000FFC3,
                    0xD63DC7EC, 0xC3BFC718, 0x00803DC7, 0x0000FF80,
                    0xFFFDDFEE, 0xC3EFFDFF, 0x00603DDF, 0x0000FFC3,
                    0xFFFDDFEC, 0xC3EFFDFF, 0x40603DDF, 0x0000FFC3,
                    0xFFFDDFEC, 0xC3FFFDFF, 0x00803DCF, 0x0000FFC3,
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0xFFFFFFFE, 0x07FF7FFF, 0x03FF7FFF, 0x00000000,
                    0xFEF02596, 0x3BFF6CAE, 0x03FF3F5F, 0x00000000,
                    0x03000000, 0xC2A003FF, 0xFFFFFEFF, 0xFFFE03FF,
                    0xFEBF0FDF, 0x02FE3FFF, 0x00000000, 0x00000000,
                    0x00000000, 0x00000000, 0x00000000, 0x00000000,
                    0x00000000, 0x00000000, 0x1FFF0000, 0x00000002,
                    0x000000A0, 0x003EFFFE, 0xFFFFFFFE, 0xFFFFFFFF,
                    0x661FFFFF, 0xFFFFFFFE, 0xFFFFFFFF, 0x77FFFFFF
            };


    public static String encodeName(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }

        StringBuilder sb = new StringBuilder();
        int length = name.length();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (isInvalid(c, i == 0)) {
                //TODO: [by yl] FIX THIS!!!!!
                sb.append(String.format("_x{0:X4}_", (int) c));
            } else if (c == '_' && i + 6 < length && name.charAt(i + 1) == 'x' &&
                    name.charAt(i + 6) == '_') {
                sb.append("_x005F_");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String DecodeName(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        int pos = name.indexOf('_');
        if (pos == -1 || pos + 6 >= name.length()) {
            return name;
        }
        if ((name.charAt(pos + 1) != 'X' && name.charAt(pos + 1) != 'x') ||
                name.charAt(pos + 6) != '_') {
            return name.charAt(0) + DecodeName(name.substring(1));
        }
        return name.substring(0, pos) + TryDecoding(name.substring(pos + 1));
    }

    private static String TryDecoding(String s) {
        if (s == null || s.length() < 6) {
            return s;
        }
        char c;
        try {
            c = (char) Integer.parseInt(s.substring(1, 4), 16);
        } catch (NumberFormatException e) {
            return s.charAt(0) + DecodeName(s.substring(1));
        }
        if (s.length() == 6) {
            return "" + c;
        }
        return c + DecodeName(s.substring(6));
    }

    private static boolean isInvalid(char c, boolean firstOnlyLetter) {
        if (c == ':')// Special case. allowed in encodeName, but encoded in encodeLocalName
        {
            return false;
        }

        if (firstOnlyLetter) {
            return !isFirstNameChar(c);
        } else {
            return !isNameChar(c);
        }
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public static boolean isNameChar(int ch) {
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
            return true;
        } else if (ch <= 0xFFFF) {
            return (NAME_BITMAP[(NAME_PAGES[ch >> 8] << 3) + ((ch & 0xFF) >> 5)] &
                    (1 << (ch & 0x1F))) != 0;
        } else {
            return false;
        }
    }

    public static boolean isFirstNameChar(int ch) {
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
            return true;
        } else if (ch <= 0xFFFF) {
            return (NAME_BITMAP[(FIRST_NAME_PAGES[ch >> 8] << 3) + ((ch & 0xFF) >> 5)] &
                    (1 << (ch & 0x1F))) != 0;
        }

        return false;
    }

}