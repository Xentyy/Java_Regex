/**
*
* @author Melih Can Şengün g211210034
* @since 04.04.2024
* <p>
* Programın çalıştığı ana sınıf
* </p>
*/



package pdp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Program {

    public static void main(String[] args) {
        try {
            BufferedReader okuyucu = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("GitHub deposunun linkini girin: ");
            String repoLink = okuyucu.readLine().trim();
            
            // Depoyu klonlar.
            File repoDizini = new File(repoLink.substring(repoLink.lastIndexOf("/") + 1).replaceAll("\\.git$", ""));
            
            // Eğer depo zaten varsa önceki versiyonunu siler
            if (repoDizini.exists() && repoDizini.isDirectory()) {
                diziniSil(repoDizini);
            }

            @SuppressWarnings("deprecation")
			Process islem = Runtime.getRuntime().exec("git clone " + repoLink);
            int kontrol = islem.waitFor();
            if (kontrol == 0) {
                System.out.println("Depo başarıyla klonlandı.\n");
            } else {
                System.out.println("Depo klonlanırken bir hata oluştu. Lütfen linki kontrol edin.");
                return;
            }
            
            // Depo içindeki java uzantılı dosyalarını analiz eder
            depoAnalizEt(repoDizini);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    // Depo içindeki java dosyalarını analiz eden fonksiyon
    private static void depoAnalizEt(File dizin) {
        if (dizin.isDirectory()) {
            File[] dosyalar = dizin.listFiles();
            if (dosyalar != null) {
                for (File dosya : dosyalar) {
                    if (dosya.isDirectory()) {
                        depoAnalizEt(dosya);
                    } else {
                        if (dosya.getName().endsWith(".java")) {
                            if (sinifIceriyorMu(dosya)) {
                                javaDosyasiniAnalizEt(dosya);
                            } else {
                                //System.out.println("Dosya içinde sınıf bulunamadı: ";
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Java dosyasında sınıf olup olmadığını kontrol eden fonksiyon
    private static boolean sinifIceriyorMu(File dosya) {
        try {
            String icerik = new String(Files.readAllBytes(dosya.toPath()));
            return icerik.contains("class ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    
    // Java dosyasının sonuçları raporlayan fonksyion
    private static void javaDosyasiniAnalizEt(File dosya) {
        try {
            String icerik = new String(Files.readAllBytes(dosya.toPath()));
            int toplamSatirlar = icerik.split("\r\n|\r|\n").length;
            int kodSatirlari = kodSatirSayisiniHesapla(icerik);
            int javadocSatirlari = javadocSatirSayisiniHesapla(icerik);
            int yorumSatirlari = yorumSatirSayisiniHesapla(icerik);
            int fonksiyonSayisi = fonksiyonSayisiniHesapla(icerik);

            double yorumSapma;

            if (fonksiyonSayisi == 0) {
                yorumSapma = -100;
            } else {
                double YG = ((javadocSatirlari + yorumSatirlari) * 0.8) / fonksiyonSayisi;
                double YH = (kodSatirlari / fonksiyonSayisi) * 0.3;

                yorumSapma = ((100 * YG) / YH) - 100;
            }
            
            // Analiz sonuçlarını ekrana yazdırır
            System.out.println("Sınıf: " + dosya.getName());
            System.out.println("Javadoc Satır Sayısı: " + javadocSatirlari);
            System.out.println("Yorum Satır Sayısı: " + yorumSatirlari);
            System.out.println("Kod Satır Sayısı: " + kodSatirlari);
            System.out.println("LOC: " + toplamSatirlar);
            System.out.println("Fonksiyon Sayısı: " + fonksiyonSayisi);
            System.out.println("Yorum Sapma Yüzdesi: %" + yorumSapma);
            System.out.println("-----------------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Kod satir sayisini hesaplayan fonksyion
    private static int kodSatirSayisiniHesapla(String icerik) {
        String[] satirlar = icerik.split("\r\n|\r|\n");
        int kodSatirlari = 0;

        boolean yorumBlokuIcerisinde = false;

        for (String satir : satirlar) {
            satir = satir.trim();

            if (!satir.isEmpty()) {
                if (!satir.startsWith("//") && !(yorumBlokuIcerisinde || satir.startsWith("/*"))) {
                    kodSatirlari++;
                }

                if (satir.startsWith("/*") && !satir.endsWith("*/")) {
                    yorumBlokuIcerisinde = true;
                } else if (yorumBlokuIcerisinde && satir.endsWith("*/")) {
                    yorumBlokuIcerisinde = false;
                }
            }
        }

        return kodSatirlari;
    }

    //Javadoc satir sayisini hesaplayan fonksyion
    private static int javadocSatirSayisiniHesapla(String icerik) {
        Pattern desen = Pattern.compile("/\\*\\*([^*]|(\\*+[^*/]))*\\*+/");
        Matcher eslestirici = desen.matcher(icerik);
        int javadocSatirlari = 0;
        while (eslestirici.find()) {
            String[] javadocBloku = eslestirici.group().split("\r\n|\r|\n");
            javadocSatirlari += javadocBloku.length - 2;
        }
        return javadocSatirlari;
    }
    
    //Yorum satir sayisini hesaplayan fonksiyon
    private static int yorumSatirSayisiniHesapla(String icerik) {
        Pattern yorumDeseni = Pattern.compile("//.*|/\\*(?:[^*]|\\*(?!/))*\\*/");
        Matcher eslestirici = yorumDeseni.matcher(icerik);
        int yorumSatirlari = 0;
        boolean javadocIcerisinde = false;

        while (eslestirici.find()) {
            String eslesme = eslestirici.group();
            if (eslesme.startsWith("//")) {
                yorumSatirlari++;
            } else if (eslesme.startsWith("/*")) {
                if (eslesme.startsWith("/**")) {
                    if (!eslesme.endsWith("*/")) {
                        javadocIcerisinde = true;
                    }
                } else {
                    if (!eslesme.endsWith("*/")) {
                        yorumSatirlari++;
                    } else {
                        if (!javadocIcerisinde) {
                            String[] satirlar = eslesme.split("\r\n|\r|\n");
                            yorumSatirlari += satirlar.length - 1;
                        } else {
                            javadocIcerisinde = false;
                        }
                    }
                }
            } else if (eslesme.endsWith("*/")) {
                if (javadocIcerisinde) {
                    javadocIcerisinde = false;
                }
            } else if (javadocIcerisinde) {
                String[] satirlar = eslesme.split("\r\n|\r|\n");
                yorumSatirlari += satirlar.length - 1;
            }
        }

        return yorumSatirlari;
    }
    
    // Fonksiyon sayısını hesaplayan fonksyiyon
    private static int fonksiyonSayisiniHesapla(String icerik) {
        Pattern desen = Pattern.compile("(\\b[a-zA-Z_]\\w*\\s+\\b[a-zA-Z_]\\w*\\s*\\([^)]*\\)\\s*\\{)|(\\b[a-zA-Z_]\\w*\\s+\\b[a-zA-Z_]\\w*\\s*\\([^)]*\\)\\s*\\[\\])");
        Matcher eslestirici = desen.matcher(icerik);
        int fonksiyonSayisi = 0;
        while (eslestirici.find()) {
            fonksiyonSayisi++;
        }
        return fonksiyonSayisi;
    }
    
    // Depo içindeki dosyaları silen fonksiyon
    private static void diziniSil(File dizin) {
        if (dizin.isDirectory()) {
            File[] dosyalar = dizin.listFiles();
            if (dosyalar != null) {
                for (File dosya : dosyalar) {
                    diziniSil(dosya);
                }
            }
        }
        dizin.delete();
    }
}
