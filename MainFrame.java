import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

// ==========================================
// 1. KELAS MODEL: MATA KULIAH (VERTEX)
// ==========================================
class MataKuliah {
    String kodeMK;
    String namaMK;
    int sks;
    int semester;

    // Atribut untuk Syarat Khusus Akademia
    int minSksSyarat = 0;       
    int minSemesterSyarat = 1;  

    int inDegree = 0;
    int bobotMasaDepan = 0;
    boolean sudahDikira = false;

    boolean isLulus = false;
    boolean isMengulang = false;

    public MataKuliah(String kodeMK, String namaMK, int sks, int semester) {
        this.kodeMK = kodeMK;
        this.namaMK = namaMK;
        this.sks = sks;
        this.semester = semester;
    }
}

// ==========================================
// 2. KELAS MODEL: MAHASISWA
// ==========================================
class Mahasiswa {
    String nama;
    int semesterSemasa;
    double ipk;
    
    Set<String> setLulus = new HashSet<>();
    Set<String> setMengulang = new HashSet<>();

    public Mahasiswa(String nama, int semesterSemasa, double ipk) {
        this.nama = nama;
        this.semesterSemasa = semesterSemasa;
        this.ipk = ipk;
    }

    public int getBatasSKS() {
        if (ipk >= 3.00) return 24;
        if (ipk >= 2.00) return 20;
        return 18;
    }
}

// ==========================================
// 3. KELAS BACKEND: ENGINE GRAF KURIKULUM
// ==========================================
class KurikulumGraph {
    Map<String, MataKuliah> nodes = new HashMap<>();
    Map<String, List<String>> adjList = new HashMap<>();

    public void tambahMataKuliah(MataKuliah mk) {
        nodes.put(mk.kodeMK, mk);
        adjList.putIfAbsent(mk.kodeMK, new ArrayList<>());
    }

    public void tambahPrasyarat(String prasyarat, String lanjutan) {
        if (adjList.containsKey(prasyarat) && nodes.containsKey(lanjutan)) {
            adjList.get(prasyarat).add(lanjutan);
            nodes.get(lanjutan).inDegree++;
        }
    }

    public void hitungSemuaBobotMasaDepan() {
        for (String kodeMK : nodes.keySet()) {
            dfsKiraBobot(kodeMK);
        }
    }

    private int dfsKiraBobot(String kodeMK) {
        MataKuliah saatIni = nodes.get(kodeMK);
        if (saatIni.sudahDikira) return saatIni.bobotMasaDepan;

        int maxBobotCabang = 0;
        for (String tetangga : adjList.get(kodeMK)) {
            MataKuliah nodeTetangga = nodes.get(tetangga);
            int bobotSementara = nodeTetangga.sks + dfsKiraBobot(tetangga);
            if (bobotSementara > maxBobotCabang) {
                maxBobotCabang = bobotSementara;
            }
        }
        saatIni.bobotMasaDepan = maxBobotCabang;
        saatIni.sudahDikira = true;
        return maxBobotCabang;
    }

    public String dapatkanLaporanRekomendasi(Mahasiswa mhs) {
        StringBuilder laporan = new StringBuilder();
        
        laporan.append("=== ANALISIS KRS UNTUK: ").append(mhs.nama.toUpperCase()).append(" ===\n");
        laporan.append("Semester Saat Ini : ").append(mhs.semesterSemasa)
               .append("\nBatas Maksimal SKS: ").append(mhs.getBatasSKS()).append(" SKS\n\n");

        // FASE 2: SINKRONISASI STATUS & HITUNG TOTAL SKS LULUS NYATA
        int totalSksLulus = 0;

        for (String kodeLulus : mhs.setLulus) {
            if (nodes.containsKey(kodeLulus)) {
                MataKuliah mk = nodes.get(kodeLulus);
                mk.isLulus = true;
                totalSksLulus += mk.sks; // Akumulasi SKS Kelulusan

                for (String lanjutan : adjList.get(kodeLulus)) {
                    nodes.get(lanjutan).inDegree--;
                }
            }
        }

        laporan.append("Total SKS Telah Lulus: ").append(totalSksLulus).append(" SKS\n\n");

        for (String kodeMengulang : mhs.setMengulang) {
            if (nodes.containsKey(kodeMengulang)) {
                nodes.get(kodeMengulang).isMengulang = true;
            }
        }

        PriorityQueue<MataKuliah> pq = new PriorityQueue<>((a, b) -> {
            if (a.isMengulang != b.isMengulang) return a.isMengulang ? -1 : 1;
            if (a.bobotMasaDepan != b.bobotMasaDepan) return Integer.compare(b.bobotMasaDepan, a.bobotMasaDepan);
            return Integer.compare(b.sks, a.sks);
        });

        // FASE 3: PENYARINGAN KANDIDAT YANG SUPER KETAT
        for (MataKuliah mk : nodes.values()) {
            if (mk.inDegree == 0 && !mk.isLulus) {
                // 1. Plafon Semester: Mencegah Floating Nodes (Matkul atas melompat ke bawah)
                if (mk.semester <= mhs.semesterSemasa) {
                    // 2. Validasi Kuantitatif: Syarat Akumulasi SKS & Syarat Minimal Semester
                    if (totalSksLulus >= mk.minSksSyarat && mhs.semesterSemasa >= mk.minSemesterSyarat) {
                        
                        boolean paritiCocok = (mk.semester % 2) == (mhs.semesterSemasa % 2);
                        if (paritiCocok || mk.isMengulang) {
                            pq.add(mk);
                        }
                    }
                }
            }
        }

        // PROSES PENGAMBILAN SKS (MURNI GREEDY BERDASARKAN ANTREAN VALID)
        int totalSKS = 0;
        int batasSKS = mhs.getBatasSKS();
        List<String> hasilKRS = new ArrayList<>();

        while (!pq.isEmpty() && totalSKS < batasSKS) {
            MataKuliah terpilih = pq.poll();

            if (totalSKS + terpilih.sks <= batasSKS) {
                hasilKRS.add(terpilih.kodeMK + " - " + terpilih.namaMK + " (" + terpilih.sks + " SKS) " 
                            + (terpilih.isMengulang ? "[MENGULANG]" : ""));
                totalSKS += terpilih.sks;

                // FIX CHAIN-TAKING BUG: 
                // Loop efek domino (inDegree--) dihapus dari sini! 
                // Mengambil prasyarat sekarang tidak membuka matkul lanjutannya di semester yang sama.
            }
        }

        laporan.append("DAFTAR REKOMENDASI MATA KULIAH:\n");
        if (hasilKRS.isEmpty()) {
            laporan.append("Tidak ada mata kuliah yang tersedia/direkomendasikan.\n");
        } else {
            for (int i = 0; i < hasilKRS.size(); i++) {
                laporan.append((i + 1)).append(". ").append(hasilKRS.get(i)).append("\n");
            }
        }
        laporan.append("---------------------------------\n");
        laporan.append("Total SKS Diambil : ").append(totalSKS).append(" / ").append(batasSKS).append("\n");

        return laporan.toString();
    }
}

// ==========================================
// 4. KELAS FRONTEND: GUI (MainFrame)
// ==========================================
public class MainFrame extends JFrame {
    private JTextField fieldNama, fieldIpk;
    private JComboBox<Integer> comboSemester;
    
    private JComboBox<String> comboInputLulus, comboInputMengulang;
    private DefaultComboBoxModel<String> modelInputLulus, modelInputMengulang;
    
    private DefaultListModel<String> modelLulus, modelMengulang;
    private JList<String> listLulus, listMengulang;
    private JTextArea areaHasil;
    
    private Set<String> setLulusGUI = new HashSet<>();
    private Set<String> setMengulangGUI = new HashSet<>();
    private KurikulumGraph graf;

    public MainFrame() {
        inisialisasiBackend();

        setTitle("Sistem Rekomendasi KRS - Sains Data");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null); 

        // Panel Profil
        JPanel panelProfil = new JPanel(new GridLayout(3, 2, 5, 10));
        panelProfil.setBorder(BorderFactory.createTitledBorder("1. Profil Mahasiswa"));
        panelProfil.add(new JLabel(" Nama Mahasiswa:"));
        fieldNama = new JTextField();
        panelProfil.add(fieldNama);
        panelProfil.add(new JLabel(" Semester Saat Ini:"));
        Integer[] sem = {1, 2, 3, 4, 5, 6, 7, 8};
        comboSemester = new JComboBox<>(sem);
        panelProfil.add(comboSemester);
        panelProfil.add(new JLabel(" IP Semester Lalu (misal: 1.9):"));
        fieldIpk = new JTextField();
        panelProfil.add(fieldIpk);

        JPanel panelTengah = new JPanel(new GridLayout(1, 2, 10, 10));

        // Setup Dropdown Dinamis
        modelInputLulus = new DefaultComboBoxModel<>();
        modelInputMengulang = new DefaultComboBoxModel<>();
        comboInputLulus = new JComboBox<>(modelInputLulus);
        comboInputMengulang = new JComboBox<>(modelInputMengulang);

        // Sub-Panel Lulus
        JPanel panelLulus = new JPanel(new BorderLayout(5, 5));
        panelLulus.setBorder(BorderFactory.createTitledBorder("2. Riwayat Matkul Lulus"));
        JPanel inputLulus = new JPanel(new BorderLayout(5, 0));
        JPanel btnPanelLulus = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton btnAddLulus = new JButton("Tambah");
        JButton btnHapusLulus = new JButton("Hapus");
        btnHapusLulus.setBackground(new Color(255, 102, 102));
        btnPanelLulus.add(btnAddLulus);
        btnPanelLulus.add(btnHapusLulus);
        inputLulus.add(comboInputLulus, BorderLayout.CENTER);
        inputLulus.add(btnPanelLulus, BorderLayout.EAST);
        modelLulus = new DefaultListModel<>();
        listLulus = new JList<>(modelLulus);
        panelLulus.add(inputLulus, BorderLayout.NORTH);
        panelLulus.add(new JScrollPane(listLulus), BorderLayout.CENTER);

        // Sub-Panel Mengulang
        JPanel panelMengulang = new JPanel(new BorderLayout(5, 5));
        panelMengulang.setBorder(BorderFactory.createTitledBorder("3. Matkul Mengulang/Gagal"));
        JPanel inputMengulang = new JPanel(new BorderLayout(5, 0));
        JPanel btnPanelMengulang = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton btnAddMengulang = new JButton("Tambah");
        JButton btnHapusMengulang = new JButton("Hapus");
        btnHapusMengulang.setBackground(new Color(255, 102, 102));
        btnPanelMengulang.add(btnAddMengulang);
        btnPanelMengulang.add(btnHapusMengulang);
        inputMengulang.add(comboInputMengulang, BorderLayout.CENTER);
        inputMengulang.add(btnPanelMengulang, BorderLayout.EAST);
        modelMengulang = new DefaultListModel<>();
        listMengulang = new JList<>(modelMengulang);
        panelMengulang.add(inputMengulang, BorderLayout.NORTH);
        panelMengulang.add(new JScrollPane(listMengulang), BorderLayout.CENTER);

        panelTengah.add(panelLulus);
        panelTengah.add(panelMengulang);

        JPanel panelBawah = new JPanel(new BorderLayout(5, 5));
        panelBawah.setBorder(BorderFactory.createTitledBorder("4. Output Algoritma"));
        JButton btnGenerate = new JButton("GENERATE REKOMENDASI KRS");
        btnGenerate.setFont(new Font("Arial", Font.BOLD, 14));
        btnGenerate.setBackground(new Color(0, 120, 215));
        btnGenerate.setForeground(Color.WHITE);
        areaHasil = new JTextArea(12, 50);
        areaHasil.setEditable(false);
        areaHasil.setFont(new Font("Monospaced", Font.PLAIN, 14)); 
        panelBawah.add(btnGenerate, BorderLayout.NORTH);
        panelBawah.add(new JScrollPane(areaHasil), BorderLayout.CENTER);

        add(panelProfil, BorderLayout.NORTH);
        add(panelTengah, BorderLayout.CENTER);
        add(panelBawah, BorderLayout.SOUTH);

        // Event Listeners
        updateDropdownBerdasarkanSemester(); 

        comboSemester.addActionListener(e -> {
            updateDropdownBerdasarkanSemester();
            setLulusGUI.clear();
            setMengulangGUI.clear();
            modelLulus.clear();
            modelMengulang.clear();
        });

        btnAddLulus.addActionListener(e -> {
            String selected = (String) comboInputLulus.getSelectedItem();
            if (selected != null) {
                String kode = selected.split(" - ")[0];
                if (setMengulangGUI.contains(kode)) {
                    JOptionPane.showMessageDialog(this, "Matkul ini sudah ada di daftar Mengulang.", "Konflik Data", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (setLulusGUI.add(kode)) modelLulus.addElement(selected);
            }
        });

        btnHapusLulus.addActionListener(e -> {
            int selectedIndex = listLulus.getSelectedIndex();
            if (selectedIndex != -1) {
                setLulusGUI.remove(modelLulus.getElementAt(selectedIndex).split(" - ")[0]);
                modelLulus.remove(selectedIndex);
            }
        });

        btnAddMengulang.addActionListener(e -> {
            String selected = (String) comboInputMengulang.getSelectedItem();
            if (selected != null) {
                String kode = selected.split(" - ")[0];
                if (setLulusGUI.contains(kode)) {
                    JOptionPane.showMessageDialog(this, "Matkul ini sudah ada di daftar Lulus.", "Konflik Data", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (setMengulangGUI.add(kode)) modelMengulang.addElement(selected);
            }
        });

        btnHapusMengulang.addActionListener(e -> {
            int selectedIndex = listMengulang.getSelectedIndex();
            if (selectedIndex != -1) {
                setMengulangGUI.remove(modelMengulang.getElementAt(selectedIndex).split(" - ")[0]);
                modelMengulang.remove(selectedIndex);
            }
        });

        btnGenerate.addActionListener(e -> prosesAlgoritma());
    }

    private void updateDropdownBerdasarkanSemester() {
        int currentSem = (Integer) comboSemester.getSelectedItem();
        modelInputLulus.removeAllElements();
        modelInputMengulang.removeAllElements();

        List<String> daftarFilter = new ArrayList<>();
        for (MataKuliah mk : graf.nodes.values()) {
            if (mk.semester <= currentSem) {
                daftarFilter.add(mk.kodeMK + " - " + mk.namaMK);
            }
        }
        Collections.sort(daftarFilter);
        for (String mkStr : daftarFilter) {
            modelInputLulus.addElement(mkStr);
            modelInputMengulang.addElement(mkStr);
        }
    }

    private void inisialisasiBackend() {
        graf = new KurikulumGraph();
        
        // SEMESTER 1
        graf.tambahMataKuliah(new MataKuliah("S1_AGM", "Pendidikan Agama", 2, 1));
        graf.tambahMataKuliah(new MataKuliah("S1_DP", "Dasar Pemrograman", 4, 1));
        graf.tambahMataKuliah(new MataKuliah("S1_MD", "Matematika Dasar", 3, 1));
        graf.tambahMataKuliah(new MataKuliah("S1_ORKOM", "Organisasi & Arsitektur Komputer", 3, 1));
        graf.tambahMataKuliah(new MataKuliah("S1_BING", "Bahasa Inggris", 2, 1));
        graf.tambahMataKuliah(new MataKuliah("S1_STAT", "Statistika Sains Data", 3, 1));
        graf.tambahMataKuliah(new MataKuliah("S1_BIND", "Bahasa Indonesia", 2, 1));

        // SEMESTER 2
        graf.tambahMataKuliah(new MataKuliah("S2_ML", "Matematika Lanjut", 3, 2));
        graf.tambahMataKuliah(new MataKuliah("S2_BD", "Perancangan Basis Data", 3, 2));
        graf.tambahMataKuliah(new MataKuliah("S2_ALIN", "Aljabar Linier Sains Data", 3, 2));
        graf.tambahMataKuliah(new MataKuliah("S2_SDA", "Struktur Data & Algoritma", 3, 2));
        graf.tambahMataKuliah(new MataKuliah("S2_VISDAT", "Metode Visualisasi Data", 3, 2));
        graf.tambahMataKuliah(new MataKuliah("S2_PKN", "Pendidikan Kewarganegaraan", 2, 2));
        graf.tambahMataKuliah(new MataKuliah("S2_SO", "Sistem Operasi", 2, 2));

        // SEMESTER 3
        graf.tambahMataKuliah(new MataKuliah("S3_MATDIS", "Matematika Diskrit & Teori Graph", 3, 3));
        graf.tambahMataKuliah(new MataKuliah("S3_KB", "Kecerdasan Buatan", 3, 3));
        graf.tambahMataKuliah(new MataKuliah("S3_SMBD", "Sistem Manajemen Basis Data", 3, 3));
        graf.tambahMataKuliah(new MataKuliah("S3_JARKOM", "Jaringan Komputer", 3, 3));
        graf.tambahMataKuliah(new MataKuliah("S3_PANC", "Pancasila", 2, 3));
        graf.tambahMataKuliah(new MataKuliah("S3_PEMSIS", "Pemodelan & Simulasi", 3, 3));
        graf.tambahMataKuliah(new MataKuliah("S3_DAA", "Desain & Analisis Algoritma", 3, 3));

        // SEMESTER 4
        graf.tambahMataKuliah(new MataKuliah("S4_INFRA", "Infrastruktur Platform Big Data", 3, 4));
        graf.tambahMataKuliah(new MataKuliah("S4_DATMIN", "Data Mining", 3, 4));
        graf.tambahMataKuliah(new MataKuliah("S4_MDI", "Manajemen Data Enterprise", 3, 4));
        graf.tambahMataKuliah(new MataKuliah("S4_KOMTER", "Komputasi Terdistribusi", 3, 4));
        graf.tambahMataKuliah(new MataKuliah("S4_RSI", "Rekayasa Sistem Informasi", 3, 4));
        graf.tambahMataKuliah(new MataKuliah("S4_KEAMANAN", "Keamanan Data & Aplikasi", 3, 4));
        graf.tambahMataKuliah(new MataKuliah("S4_ETIKA", "Etika Profesi Sains Data", 2, 4));

        // SEMESTER 5
        graf.tambahMataKuliah(new MataKuliah("S5_KBR", "Knowledge Based & Reasoning", 3, 5));
        graf.tambahMataKuliah(new MataKuliah("S5_CLOUD", "Teknologi Cloud Big Data", 3, 5));
        graf.tambahMataKuliah(new MataKuliah("S5_ML", "Machine Learning", 3, 5));
        graf.tambahMataKuliah(new MataKuliah("S5_REKBIG", "Rekayasa Organisasi Sistem Big Data", 3, 5));
        graf.tambahMataKuliah(new MataKuliah("S5_DESAPP", "Desain Aplikasi Big Data", 3, 5));

        // SEMESTER 6
        graf.tambahMataKuliah(new MataKuliah("S6_METPEN", "Metode Penelitian", 3, 6));
        graf.tambahMataKuliah(new MataKuliah("S6_NLP", "Natural Language Processing", 3, 6));
        graf.tambahMataKuliah(new MataKuliah("S6_MANPRO", "Manajemen Proyek", 3, 6));
        graf.tambahMataKuliah(new MataKuliah("S6_KMM", "KMM (Magang)", 3, 6));
        graf.tambahMataKuliah(new MataKuliah("S6_CAPS", "Capstone Project", 3, 6));
        graf.tambahMataKuliah(new MataKuliah("S6_PM3", "Pilihan Minat 3", 3, 6));
        graf.tambahMataKuliah(new MataKuliah("S6_PM4", "Pilihan Minat 4", 3, 6));

        // SEMESTER 7
        graf.tambahMataKuliah(new MataKuliah("S7_KKN", "KKN", 2, 7));
        graf.tambahMataKuliah(new MataKuliah("S7_KWU", "Kewirausahaan", 2, 7));
        graf.tambahMataKuliah(new MataKuliah("S7_PB1", "Pilihan Bebas 1", 3, 7));
        graf.tambahMataKuliah(new MataKuliah("S7_PB2", "Pilihan Bebas 2", 3, 7));
        graf.tambahMataKuliah(new MataKuliah("S7_PB3", "Pilihan Bebas 3", 3, 7));
        graf.tambahMataKuliah(new MataKuliah("S7_PB4", "Pilihan Bebas 4", 3, 7));
        graf.tambahMataKuliah(new MataKuliah("S7_PB5", "Pilihan Bebas 5", 3, 7));

        // SEMESTER 8
        graf.tambahMataKuliah(new MataKuliah("S8_SKRIPSI", "Skripsi / Tugas Akhir", 6, 8));

        // RELASI PRASYARAT STANDARD
        graf.tambahPrasyarat("S1_MD", "S2_ML");      
        graf.tambahPrasyarat("S2_ML", "S3_MATDIS");  
        graf.tambahPrasyarat("S1_DP", "S2_BD");      
        graf.tambahPrasyarat("S1_DP", "S2_SDA");     
        graf.tambahPrasyarat("S2_SDA", "S3_DAA");    
        graf.tambahPrasyarat("S2_BD", "S3_SMBD");    
        graf.tambahPrasyarat("S3_SMBD", "S4_MDI");   
        graf.tambahPrasyarat("S4_MDI", "S5_DESAPP"); 
        graf.tambahPrasyarat("S1_MD", "S2_ALIN");    
        graf.tambahPrasyarat("S2_ALIN", "S3_KB");    
        graf.tambahPrasyarat("S3_KB", "S4_DATMIN");  
        graf.tambahPrasyarat("S4_DATMIN", "S5_ML");  
        graf.tambahPrasyarat("S1_ORKOM", "S2_SO");     
        graf.tambahPrasyarat("S2_SO", "S3_JARKOM");    
        graf.tambahPrasyarat("S3_JARKOM", "S4_KOMTER");
        graf.tambahPrasyarat("S1_STAT", "S2_VISDAT"); 
        graf.tambahPrasyarat("S2_VISDAT", "S3_PEMSIS"); 
        graf.tambahPrasyarat("S5_ML", "S6_NLP");         
        graf.tambahPrasyarat("S5_DESAPP", "S6_CAPS");    
        graf.tambahPrasyarat("S6_METPEN", "S8_SKRIPSI"); 

        // ====================================================
        // IMPLEMENTASI PERSYARATAN STRATEGIS (SKS & SEMESTER)
        // ====================================================
        // KKN dan KMM (Magang) wajib mengumpulkan minimal 84 SKS Lulus
        graf.nodes.get("S7_KKN").minSksSyarat = 84;
        graf.nodes.get("S6_KMM").minSksSyarat = 84;

        // Aturan MBKM & Pilihan: Hanya boleh aktif mulai di Semester 5 atau 6
        graf.nodes.get("S6_PM3").minSemesterSyarat = 5;
        graf.nodes.get("S6_PM4").minSemesterSyarat = 5;
        graf.nodes.get("S7_PB1").minSemesterSyarat = 5;
        graf.nodes.get("S7_PB2").minSemesterSyarat = 5;
        graf.nodes.get("S7_PB3").minSemesterSyarat = 5;
        graf.nodes.get("S7_PB4").minSemesterSyarat = 5;
        graf.nodes.get("S7_PB5").minSemesterSyarat = 5;

        graf.hitungSemuaBobotMasaDepan();
    }

    private void prosesAlgoritma() {
        try {
            inisialisasiBackend(); 

            String nama = fieldNama.getText();
            if (nama.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama tidak boleh kosong!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int semester = (Integer) comboSemester.getSelectedItem();
            double ipk = Double.parseDouble(fieldIpk.getText());

            Mahasiswa mhs = new Mahasiswa(nama, semester, ipk);
            mhs.setLulus.addAll(setLulusGUI);
            mhs.setMengulang.addAll(setMengulangGUI);

            String laporanAkhir = graf.dapatkanLaporanRekomendasi(mhs);
            areaHasil.setText(laporanAkhir); 

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Format IPK salah! Gunakan angka desimal (misal: 3.5)", "Error Input", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan algoritma: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}