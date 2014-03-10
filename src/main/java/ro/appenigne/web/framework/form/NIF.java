package ro.appenigne.web.framework.form;

import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import ro.appenigne.web.framework.exception.InvalidField;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class NIF {
    String name;
    String regex;


    Boolean validate(String value) throws InvalidField {
        return true;
    }

    public static NIF createAT() {
        NIF AT = new NIF() {
            {
                this.name = "AT";
                this.regex = "^U[0-9]{8}$";
            }

            @Override
            Boolean validate(String value) {
                ArrayList<Integer> N = toIntArray(value.substring(1, 8));
                Integer C = Integer.parseInt(value.substring(8, 9));
                Integer S = N.get(0)
                        + diez(2, N.get(1))
                        + N.get(2)
                        + diez(2, N.get(3))
                        + N.get(4)
                        + diez(2, N.get(5))
                        + N.get(6);
                Integer check = 10 - ((S + 4) % 10);
                if (check == 10) {
                    check = 0;
                }
                return (C.equals(check));
            }
        };
        return AT;
    }

    public static NIF createBE() {
        NIF BE = new NIF() {
            {
                this.name = "BE";
                this.regex = "^[0-9]{1}[2-9]{1}[0-9]{8}$";
            }

            @Override
            Boolean validate(String value) {
                Integer N = Integer.parseInt(value.substring(0, 8));
                Integer C = Integer.parseInt(value.substring(8, 10));
                return (C.equals(97 - (N % 97)));
            }
        };
        return BE;
    }

    public static NIF createBG() {
        // http://en.wikipedia.org/wiki/Uniform_civil_number
        NIF BE = new NIF() {
            {
                this.name = "BG";
                this.regex = "^[0-9]{10}$";
            }

            @Override
            Boolean validate(String value) {
                ArrayList<Integer> N = toIntArray(value);
                if ((N.get(0) == 2) || (N.get(0) == 3)) {
                    if ((N.get(1) != 2) || (N.get(2) != 2)) {
                        return false;
                    }
                }
                Integer S = (2 * N.get(0))
                        + (4 * N.get(1))
                        + (8 * N.get(2))
                        + (5 * N.get(3))
                        + (10 * N.get(4))
                        + (9 * N.get(5))
                        + (7 * N.get(6))
                        + (3 * N.get(7))
                        + (6 * N.get(8));
                Integer check = S % 11;
                if ((check == 11) || (check == 10)) {
                    check = 0;
                }
                return (check.equals(N.get(9)));
            }
        };
        return BE;
    }

    public static NIF createCY() {
        NIF CY = new NIF() {
            {
                this.name = "CY";
                this.regex = "^[0|1|3|4|5|9]{1}[0-9]{7}[A-Z]{1}$";
            }

            public Integer translateNrCY(Integer nr) {
                switch (nr) {
                    case 0:
                        return 1;
                    case 1:
                        return 0;
                    case 2:
                        return 5;
                    case 3:
                        return 7;
                    case 4:
                        return 9;
                    case 5:
                        return 13;
                    case 6:
                        return 15;
                    case 7:
                        return 17;
                    case 8:
                        return 19;
                    default:
                        return 21;
                }
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 8));
                char C = value.charAt(8);
                N.set(0, this.translateNrCY(N.get(0)));
                N.set(2, this.translateNrCY(N.get(2)));
                N.set(4, this.translateNrCY(N.get(4)));
                N.set(6, this.translateNrCY(N.get(6)));
                Integer S = N.get(0) + N.get(1) + N.get(2) + N.get(3) + N.get(4) + N.get(5) + N.get(6) + N.get(7);
                char check = (char) ((S % 26) + 65);
                return (check == C);
            }
        };
        return CY;
    }

    public static NIF createCZ() {
        NIF CZ = new NIF() {
            {
                this.name = "CZ";
                this.regex = "^6[0-9]{8}$";
            }

            @Override
            Boolean validate(String value) {
                ArrayList<Integer> N = toIntArray(value.substring(0, 8));
                Integer C = Integer.parseInt(value.substring(8, 9));
                Integer S = (8 * N.get(1))
                        + (7 * N.get(2))
                        + (6 * N.get(3))
                        + (5 * N.get(4))
                        + (4 * N.get(5))
                        + (3 * N.get(6))
                        + (2 * N.get(7));
                Integer check = 9 - ((11 - (S % 11)) % 10);
                return (C.equals(check));
            }
        };
        return CZ;
    }

    // 9 cifre - persoane fizice
    public static NIF createCZ1() {
        NIF CZ1 = new NIF() {
            {
                this.name = "CZ";
                this.regex = "^(([0-4][0-9]|5[0-3])|([89][0-9]))((0[1-9]|1[012])|(5[1-9]|6[012]))(0[1-9]|[12][0-9]|3[01])[0-9]{3}$";
            }

            @Override
            Boolean validate(String value) {
                Integer year = Integer.parseInt(value.substring(0, 2));
                Integer month = Integer.parseInt(value.substring(2, 4));
                Integer day = Integer.parseInt(value.substring(4, 6));
                return (validMonthDays(day, month, year));
            }
        };
        return CZ1;
    }

    public static NIF createCZ2() {
        NIF CZ2 = new NIF() {
            {
                this.name = "CZ";
                this.regex = "^(5[4-9]|[6-9][0-9])((0[1-9]|1[012])|(5[1-9]|6[012]))(0[1-9]|[12][0-9]|3[01])[0-9]{4}$";
            }

            @Override
            Boolean validate(String value) {
                Integer year = Integer.parseInt(value.substring(0, 2));
                Integer month = Integer.parseInt(value.substring(2, 4));
                Integer day = Integer.parseInt(value.substring(4, 6));
                return (validMonthDays(day, month, year));
            }
        };
        return CZ2;
    }

    public static NIF createDE() {
        NIF DE = new NIF() {
            {
                this.name = "DE";
                this.regex = "^[1-9]{8}[0-9]{1}$";
            }

            @Override
            Boolean validate(String value) {

                Integer C = Integer.parseInt(value.substring(8, 9));
                Integer S = 0;
                for (int i = 0; i < 8; i++) {
                    S = (2 * (((Integer.parseInt(value.substring(i, i + 1)) + S + 9) % 10) + 1)) % 11;
                }
                Integer check = 11 - S;
                if (check == 10) {
                    check = 0;
                }
                return (check.equals(C));
            }
        };
        return DE;
    }

    public static NIF createDK() {
        NIF DK = new NIF() {
            {
                this.name = "DK";
                this.regex = "^[1-9]{1}[0-9]{7}$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 8));
                Integer S = (2 * N.get(0))
                        + (7 * N.get(1))
                        + (6 * N.get(2))
                        + (5 * N.get(3))
                        + (4 * N.get(4))
                        + (3 * N.get(5))
                        + (2 * N.get(6))
                        + N.get(7);
                return ((S % 11) == 0);
            }
        };
        return DK;
    }

    public static NIF createEE() {
        NIF EE = new NIF() {
            {
                this.name = "EE";
                this.regex = "^[0-9]{9}$";
            }

            @Override
            Boolean validate(String value) {
                ArrayList<Integer> N = toIntArray(value.substring(0, 8));
                Integer C = Integer.parseInt(value.substring(8, 9));
                Integer S = (3 * N.get(0))
                        + (7 * N.get(1))
                        + (1 * N.get(2))
                        + (3 * N.get(3))
                        + (7 * N.get(4))
                        + (1 * N.get(5))
                        + (3 * N.get(6))
                        + (7 * N.get(7));
                Integer check = 10 - (S % 10);
                if (check == 10) {
                    check = 0;
                }
                return (C.equals(check));
            }
        };
        return EE;
    }

    public static NIF createES() {
        NIF ES = new NIF() {
            {
                this.name = "ES";
                this.regex = "^[0-9]{8}[TRWAGMYFPDXBNJZSQVHLCKE]$";
            }

            @Override
            Boolean validate(String value) {
                char C = value.charAt(8);
                Integer check = 1 + (Integer.parseInt(value.substring(0, 8)) % 23);

                return (C == translateNrES(check));
            }
        };
        return ES;
    }

    public static NIF createES1() {
        NIF ES1 = new NIF() {
            {
                this.name = "ES";
                this.regex = "^[KLMX][0-9]{7}[TRWAGMYFPDXBNJZSQVHLCKE]$";
            }

            @Override
            Boolean validate(String value) {
                char C = value.charAt(8);
                Integer check = 1 + (Integer.parseInt(value.substring(1, 8)) % 23);

                return (C == translateNrES(check));
            }
        };
        return ES1;
    }

    public static NIF createFI() {
        NIF FI = new NIF() {
            {
                this.name = "FI";
                this.regex = "^[0-9]{8}$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 7));
                Integer C = Integer.parseInt(value.substring(7, 8));
                Integer S = (7 * N.get(0))
                        + (9 * N.get(1))
                        + (10 * N.get(2))
                        + (5 * N.get(3))
                        + (8 * N.get(4))
                        + (4 * N.get(5))
                        + (2 * N.get(6));
                Integer check = 11 - (S % 11);
                if (check == 11) {
                    check = 0;
                }
                return check != 10 && (C.equals(check));
            }
        };
        return FI;
    }

    public static NIF createFR() {
        NIF FR = new NIF() {
            {
                this.name = "FR";
                this.regex = "^[0-9]{2}[1-9]{9}$";
            }

            @Override
            Boolean validate(String value) {
                Integer N = Integer.parseInt(value.substring(2, 11));
                Integer C = Integer.parseInt(value.substring(0, 2));
                Long check = (N.longValue() * 100) + 12;
                return (C == (check % 97));
            }
        };
        return FR;
    }

    public static NIF createFR1() {
        NIF FR1 = new NIF() {
            {
                this.name = "FR";
                this.regex = "^(([A-HJ-NP-Z][0-9])|([0-9][A-HJ-NP-Z]))[0-9]{9}$";
            }

            public Integer translateNrFr(char ch) {
                // C{0-9,A-H,J-N,P-Z} → C{0-33}
                if ((ch >= 48) && (ch <= 57)) {
                    return ch - 48;
                } else {
                    if ((ch >= 65) && (ch <= 72)) {
                        return ch - 55; // A ->10 H->17
                    } else if ((ch >= 74) && (ch <= 78)) {
                        return ch - 56; // J->18 N->22
                    } else {
                        // if ((int)ch >= 80 && (int)ch <= 90) //P=80-Z=90
                        return ch - 57; // P->23 Z->33
                    }
                }
            }

            @Override
            Boolean validate(String value) {

                Integer C1 = this.translateNrFr(value.charAt(0));
                Integer C2 = this.translateNrFr(value.charAt(1));
                Integer S = 0;
                if (C1 < 10) {
                    S = ((C1 * 24) + C2) - 10;
                }
                if (C1 > 9) {
                    S = ((C1 * 34) + C2) - 100;
                }
                Integer N = Integer.parseInt(value.substring(2, 11));
                Integer X = S % 11;
                S += 1;
                Integer Y = (N + S) % 11;
                return (X.equals(Y));
            }
        };
        return FR1;
    }

    public static NIF createGB() {
        NIF GB = new NIF() {
            {
                this.name = "GB";
                this.regex = "^(000000[1-9]|00000[1-9][0-9]|0000[1-9][0-9][0-9]|000[1-9][0-9][0-9][0-9]|001[0-9][0-9][0-9][0-9]|[1-9][0-9][0-9][0-9][0-9][0-9][0-9])(([0-8][0-9])|(9[0-6]))$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 9));
                Integer S = (8 * N.get(0))
                        + (7 * N.get(1))
                        + (6 * N.get(2))
                        + (5 * N.get(3))
                        + (4 * N.get(4))
                        + (3 * N.get(5))
                        + (2 * N.get(6))
                        + (10 * N.get(7))
                        + N.get(8);
                return ((S % 97) == 0);
            }
        };
        return GB;
    }

    public static NIF createGB1() {
        NIF GB1 = new NIF() {
            {
                this.name = "GB";
                this.regex = "^00[01](000000[1-9]|00000[1-9][0-9]|0000[1-9][0-9][0-9]|000[1-9][0-9][0-9][0-9]|001[0-9][0-9][0-9][0-9]|[1-9][0-9][0-9][0-9][0-9][0-9][0-9])(([0-8][0-9])|(9[0-6]))$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 12));
                Integer S = (8 * N.get(3))
                        + (7 * N.get(4))
                        + (6 * N.get(5))
                        + (5 * N.get(6))
                        + (4 * N.get(7))
                        + (3 * N.get(8))
                        + (2 * N.get(9))
                        + (10 * N.get(10))
                        + N.get(11);
                return ((S % 97) == 0);
            }
        };
        return GB1;
    }

    public static NIF createGR() {
        NIF GR = new NIF() {
            {
                this.name = "GR";
                this.regex = "^[0-9]{8}$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 8));
                Integer C = N.get(7);
                Integer S = (128 * N.get(0))
                        + (64 * N.get(1))
                        + (32 * N.get(2))
                        + (16 * N.get(3))
                        + (8 * N.get(4))
                        + (4 * N.get(5))
                        + (2 * N.get(6));
                Integer check = S % 11;
                if (check == 10) {
                    check = 0;
                }
                return (C.equals(check));
            }
        };
        return GR;
    }

    public static NIF createGR1() {
        NIF GR1 = new NIF() {
            {
                this.name = "GR";
                this.regex = "[0-9]{9}";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 9));
                Integer C = N.get(8);
                Integer S = (256 * N.get(0))
                        + (128 * N.get(1))
                        + (64 * N.get(2))
                        + (32 * N.get(3))
                        + (16 * N.get(4))
                        + (8 * N.get(5))
                        + (4 * N.get(6))
                        + (2 * N.get(7));
                Integer check = S % 11;
                if (check == 10) {
                    check = 0;
                }
                return (C.equals(check));
            }
        };
        return GR1;
    }

    public static NIF createHU() {
        NIF HU = new NIF() {
            {
                this.name = "HU";
                this.regex = "^[1-9][0-9]{7}$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 8));
                Integer C = N.get(7);
                Integer S = (9 * N.get(0))
                        + (7 * N.get(1))
                        + (3 * N.get(2))
                        + (1 * N.get(3))
                        + (9 * N.get(4))
                        + (7 * N.get(5))
                        + (3 * N.get(6));
                Integer check = 10 - (S % 10);
                if (check == 10) {
                    check = 0;
                }
                return check != 10 && (C.equals(check));
            }
        };
        return HU;
    }

    public static NIF createIE() {
        NIF IE = new NIF() {
            {
                this.name = "IE";
                this.regex = "^[7-9]([A-Z]|\\+|\\*)[0-9]{5}[A-W]$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(2, 7));
                char C = value.charAt(7);
                Integer S = (7 * N.get(0))
                        + (6 * N.get(1))
                        + (5 * N.get(2))
                        + (4 * N.get(3))
                        + (3 * N.get(4))
                        + (2 * Integer.parseInt(value.substring(0, 1)));
                Integer check = S % 23;
                return (C == ((check == 0) ? 'W' : (char) (64 + check)));
            }
        };
        return IE;
    }

    public static NIF createIE1() {
        NIF IE1 = new NIF() {
            {
                this.name = "IE";
                this.regex = "^[1-9]{7}[A-W]$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 7));
                char C = value.charAt(7);
                Integer S = (8 * N.get(0))
                        + (7 * N.get(1))
                        + (6 * N.get(2))
                        + (5 * N.get(3))
                        + (4 * N.get(4))
                        + (3 * N.get(5))
                        + (2 * N.get(6));
                Integer check = S % 23;
                return (C == ((check == 0) ? 'W' : (char) (64 + check)));
            }
        };
        return IE1;
    }

    public static NIF createIT() {
        NIF IT = new NIF() {
            {
                this.name = "IT";
                this.regex = "^[1-9]{7}(00[1-9]|0[1-9][0-9]|100|120|121)[0-9]$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 10));
                Integer C = Integer.parseInt(value.substring(10, 11));
                Integer S = N.get(0)
                        + diez(2, N.get(1))
                        + N.get(2)
                        + diez(2, N.get(3))
                        + N.get(4)
                        + diez(2, N.get(5))
                        + N.get(6)
                        + diez(2, N.get(7))
                        + N.get(8)
                        + diez(2, N.get(9));
                Integer check = 10 - (S % 10);
                if (check == 10) {
                    check = 0;
                }
                return (C.equals(check));
            }
        };
        return IT;
    }

    public static NIF createLT() {
        NIF LT = new NIF() {
            {
                this.name = "LT";
                this.regex = "^[0-9]{10}1[0-9]$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 11));
                Integer C = Integer.parseInt(value.substring(11, 12));
                Integer S = (1 * N.get(0))
                        + (2 * N.get(1))
                        + (3 * N.get(2))
                        + (4 * N.get(3))
                        + (5 * N.get(4))
                        + (6 * N.get(5))
                        + (7 * N.get(6))
                        + (8 * N.get(7))
                        + (9 * N.get(8))
                        + (1 * N.get(9))
                        + (2 * N.get(10));
                if ((S % 11) == 10) {
                    S = (3 * N.get(0))
                            + (4 * N.get(1))
                            + (5 * N.get(2))
                            + (6 * N.get(3))
                            + (7 * N.get(4))
                            + (8 * N.get(5))
                            + (9 * N.get(6))
                            + (1 * N.get(7))
                            + (2 * N.get(8))
                            + (3 * N.get(9))
                            + (4 * N.get(10));
                }
                Integer check = (S % 11);
                if (check == 10) {
                    check = 0;
                }
                return (C.equals(check));
            }
        };
        return LT;
    }

    public static NIF createLU() {
        NIF LU = new NIF() {
            {
                this.name = "LU";
                this.regex = "^[1-9]{6}[0-9]{2}$";
            }

            @Override
            Boolean validate(String value) {

                Integer N = Integer.parseInt(value.substring(0, 6));
                Integer C = Integer.parseInt(value.substring(6, 8));

                return (C == (N % 89));
            }
        };
        return LU;
    }

    public static NIF createLV() {
        NIF LV = new NIF() {
            {
                this.name = "LV";
                this.regex = "^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[012])[0-9]{7}$";
            }

            @Override
            Boolean validate(String value) {

                Integer year = Integer.parseInt(value.substring(4, 6));
                Integer month = Integer.parseInt(value.substring(2, 4));
                Integer day = Integer.parseInt(value.substring(0, 2));
                return (validMonthDays(day, month, year));
            }
        };
        return LV;
    }

    public static NIF createMD() {
        NIF MD = new NIF() {
            {
                this.name = "MD";
                this.regex = "^[0-9]{13}$";
            }

            @Override
            Boolean validate(String value) throws InvalidField {
                URLFetchService fetcher = URLFetchServiceFactory.getURLFetchService();
                URL url;
                try {
                    url = new URL("http://www.mtic.gov.md/WebPublic/index.php?action=person&idnp=" + value);
                    HTTPResponse response = fetcher.fetch(url);
                    if (response != null) {
                        StringBuilder html = new StringBuilder();
                        for (byte b : response.getContent()) {
                            html.append((char) b);
                        }
                        Logger log = Logger.getLogger("CheckCNPMoldova");

                        org.jsoup.nodes.Document doc = Jsoup.parse(html.toString());
                        org.jsoup.nodes.Element content = doc.html(html.toString());
                        Elements values = content.getElementsByTag("b");
                        if (values.size() != 3) {
                            log.log(Level.SEVERE, html.toString());
                            return true;
                        }
                        if (values.get(1).html().equals("DA")) {
                            return true;
                        } else if (values.get(1).html().equals("NU")) {
                            return false;
                        } else {
                            log.log(Level.SEVERE, html.toString());
                            return true;
                        }
                    }
                } catch (MalformedURLException e) {
                    Logger log = Logger.getLogger("CheckCNPMoldova");
                    log.log(Level.SEVERE, value);
                    log.log(Level.SEVERE, "", e);
                    return true;
                } catch (IOException e) {
                    Logger log = Logger.getLogger("CheckCNPMoldova");
                    log.log(Level.SEVERE, value);
                    log.log(Level.SEVERE, "", e);
                    return true;
                }
                return true;
            }
        };
        return MD;
    }

    public static NIF createMT() {
        NIF MT = new NIF() {
            {
                this.name = "MT";
                this.regex = "^(10000[1-9]|1000[1-9][0-9]|100[1-9][0-9][0-9]|10[1-9][0-9][0-9][0-9]|1[1-9][0-9][0-9][0-9][0-9]|[1-9][0-9][0-9][0-9][0-9][0-9])[0-9]{2}$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 6));
                Integer C = Integer.parseInt(value.substring(6, 8));
                Integer S = (3 * N.get(0))
                        + (4 * N.get(1))
                        + (6 * N.get(2))
                        + (7 * N.get(3))
                        + (8 * N.get(4))
                        + (9 * N.get(5));
                Integer check = 37 - (S % 37);
                return (C.equals(check));
            }
        };
        return MT;
    }

    public static NIF createNL() {
        NIF NL = new NIF() {
            {
                this.name = "NL";
                this.regex = "^[1-9]{8}[0-9]B[0-9]{2}$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 8));
                Integer C = Integer.parseInt(value.substring(8, 9));
                Integer S = (9 * N.get(0))
                        + (8 * N.get(1))
                        + (7 * N.get(2))
                        + (6 * N.get(3))
                        + (5 * N.get(4))
                        + (4 * N.get(5))
                        + (3 * N.get(6))
                        + (2 * N.get(7));
                Integer check = S % 11;
                if (check == 10) {
                    return false;
                }
                return (C.equals(check));
            }
        };
        return NL;
    }

    public static NIF createPL() {
        NIF PL = new NIF() {
            {
                this.name = "PL";
                this.regex = "^[0-9]{10}$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 9));
                Integer C = Integer.parseInt(value.substring(9, 10));
                Integer S = (6 * N.get(0))
                        + (5 * N.get(1))
                        + (7 * N.get(2))
                        + (2 * N.get(3))
                        + (3 * N.get(4))
                        + (4 * N.get(5))
                        + (5 * N.get(6))
                        + (6 * N.get(7))
                        + (7 * N.get(8));
                Integer check = S % 11;
                if (check == 10) {
                    return false;
                }
                return (C.equals(check));
            }
        };
        return PL;
    }

    public static NIF createPT() {
        NIF PT = new NIF() {
            {
                this.name = "PT";
                this.regex = "^[1-9][0-9]{8}$";
            }

            @Override
            Boolean validate(String value) {
                ArrayList<Integer> N = toIntArray(value.substring(0, 8));
                Integer C = Integer.parseInt(value.substring(8, 9));
                Integer S = (9 * N.get(0))
                        + (8 * N.get(1))
                        + (7 * N.get(2))
                        + (6 * N.get(3))
                        + (5 * N.get(4))
                        + (4 * N.get(5))
                        + (3 * N.get(6))
                        + (2 * N.get(7));
                Integer check = 11 - (S % 11);
                if ((check == 10) || (check == 11)) {
                    check = 0;
                }
                return (C.equals(check));
            }
        };
        return PT;
    }

    public static NIF createRO() {
        NIF RO = new NIF() {
            {
                this.name = "RO";
                this.regex = "^[1-9][0-9]{2}(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])[0-9]{6}$";
            }

            @Override
            Boolean validate(String value) {
                ArrayList<Integer> N = toIntArray(value.substring(0, 12));
                Integer C = Integer.parseInt(value.substring(12, 13));

                Integer year = Integer.parseInt(value.substring(1, 3));
                Integer month = Integer.parseInt(value.substring(3, 5));
                Integer day = Integer.parseInt(value.substring(5, 7));
                if (!validMonthDays(day, month, year)) {
                    return false;
                }

                Integer S = (2 * N.get(0))
                        + (7 * N.get(1))
                        + (9 * N.get(2))
                        + (1 * N.get(3))
                        + (4 * N.get(4))
                        + (6 * N.get(5))
                        + (3 * N.get(6))
                        + (5 * N.get(7))
                        + (8 * N.get(8))
                        + (2 * N.get(9))
                        + (7 * N.get(10))
                        + (9 * N.get(11));

                Integer check = S % 11;
                if (check == 10) {
                    check = 1;
                }
                return (C.equals(check));
            }
        };
        return RO;
    }

    public static NIF createSE() {
        NIF SE = new NIF() {
            {
                this.name = "SE";
                this.regex = "^[0-9]{10}(0[1-9]|[1-9][0-9])$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 9));
                Integer C = Integer.parseInt(value.substring(9, 10));
                Integer S = diez(2, N.get(0))
                        + N.get(1)
                        + diez(2, N.get(2))
                        + N.get(3)
                        + diez(2, N.get(4))
                        + N.get(5)
                        + diez(2, N.get(6))
                        + N.get(7)
                        + diez(2, N.get(8));
                Integer check = 10 - (S % 10);
                if (check == 10) {
                    check = 0;
                }
                return (C.equals(check));
            }
        };
        return SE;
    }

    public static NIF createSI() {
        NIF SI = new NIF() {
            {
                this.name = "SI";
                this.regex = "^([1-9][0-9][0-9][0-9][0-9][0-9][0-9])[0-9]$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 7));
                Integer C = Integer.parseInt(value.substring(7, 8));
                Integer S = (8 * N.get(0))
                        + (7 * N.get(1))
                        + (6 * N.get(2))
                        + (5 * N.get(3))
                        + (4 * N.get(4))
                        + (3 * N.get(5))
                        + (2 * N.get(6));
                Integer check = 11 - (S % 11);
                if (check == 10) {
                    check = 0;
                }
                if (check == 11) {
                    check = 1;
                }
                return (C.equals(check));
            }
        };
        return SI;
    }

    public static NIF createSK() {
        NIF SK = new NIF() {
            {
                this.name = "SK";
                this.regex = "^00[0-9]{8}$";
            }

            @Override
            Boolean validate(String value) {

                ArrayList<Integer> N = toIntArray(value.substring(0, 9));
                Integer C = Integer.parseInt(value.substring(9, 10));
                Integer S = (8 * N.get(2))
                        + (7 * N.get(3))
                        + (6 * N.get(4))
                        + (5 * N.get(5))
                        + (4 * N.get(6))
                        + (3 * N.get(7))
                        + (2 * N.get(8));
                Integer check = 11 - (S % 11);
                if (check == 10) {
                    check = 0;
                }
                if (check == 11) {
                    check = 1;
                }
                return (C.equals(check));
            }
        };
        return SK;
    }

    public static NIF createSK1() {
        NIF SK1 = new NIF() {
            {
                this.name = "SK";
                this.regex = "^([0-5][0-9])((0[1-9]|1[012])|(5[1-9]|6[012]))(0[1-9]|[12][0-9]|3[01])[0-9]{3}$";
            }

            @Override
            Boolean validate(String value) {
                Integer year = Integer.parseInt(value.substring(0, 2));
                Integer month = Integer.parseInt(value.substring(2, 4));
                Integer day = Integer.parseInt(value.substring(4, 6));
                return (validMonthDays(day, month, year));
            }
        };
        return SK1;
    }

    public static NIF createSK2() {
        NIF SK2 = new NIF() {
            {
                this.name = "SK";
                this.regex = "^(5[4-9]|[6-9][0-9])((0[1-9]|1[012])|(5[1-9]|6[012]))(0[1-9]|[12][0-9]|3[01])[0-9]{4}$";
            }

            @Override
            Boolean validate(String value) {
                Integer year = Integer.parseInt(value.substring(0, 2));
                Integer month = Integer.parseInt(value.substring(2, 4));
                Integer day = Integer.parseInt(value.substring(4, 6));
                return (validMonthDays(day, month, year));
            }
        };
        return SK2;
    }

    public static Integer diez(Integer nr1, Integer nr2) {
        Integer n = nr1 * nr2;
        while (n >= 9) {
            n = (n % 10) + (n / 10);
        }
        return n;
    }

    public static Boolean validMonthDays(Integer day, Integer month, Integer year) {

        if (((year % 4) != 0) && ((month == 2) || (month == 52))) {
            if (day > 28) {
                return false;
            }
        }
        if (((year % 4) == 0) && ((month == 2) || (month == 52))) {
            if (day > 29) {
                return false;
            }
        }
        switch (month) {
            case 4:
            case 6:
            case 9:
            case 11:
            case 54:
            case 56:
            case 59:
            case 61:
                if (day > 30) {
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    public static ArrayList<Integer> toIntArray(String N) {
        ArrayList<Integer> v = new ArrayList<>();
        for (int i = 0; i < N.length(); i++) {
            v.add(Integer.parseInt(N.substring(i, i + 1)));
        }
        return v;
    }

    public static char translateNrES(Integer nr) {
        // C{1-23} → C{T,R,W,A,G,M,Y,F,P,D,X,B,N,J,Z,S,Q,V,H,L,C,K,E}
        switch (nr) {
            case 1:
                return 'T';
            case 2:
                return 'R';
            case 3:
                return 'W';
            case 4:
                return 'A';
            case 5:
                return 'G';
            case 6:
                return 'M';
            case 7:
                return 'Y';
            case 8:
                return 'F';
            case 9:
                return 'P';
            case 10:
                return 'D';
            case 11:
                return 'X';
            case 12:
                return 'B';
            case 13:
                return 'N';
            case 14:
                return 'J';
            case 15:
                return 'Z';
            case 16:
                return 'S';
            case 17:
                return 'Q';
            case 18:
                return 'V';
            case 19:
                return 'H';
            case 20:
                return 'L';
            case 21:
                return 'C';
            case 22:
                return 'K';
            default:
                return 'E';
        }
    }
}
