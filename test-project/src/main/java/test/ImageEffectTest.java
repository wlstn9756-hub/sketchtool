package test;

import util.ImageUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 이미지 효과 테스트 프로그램
 *
 * 사용법:
 *   java -jar test-project.jar                    # 샘플 이미지로 테스트
 *   java -jar test-project.jar image1.jpg         # 단일 이미지 테스트
 *   java -jar test-project.jar img1.jpg img2.jpg  # 2열 그리드 테스트
 *   java -jar test-project.jar img1.jpg img2.jpg img3.jpg img4.jpg  # 2x2 그리드 테스트
 */
public class ImageEffectTest {

    private static final String OUTPUT_DIR = "output";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  🎨 이미지 효과 테스트 프로그램");
        System.out.println("========================================\n");

        // output 폴더 생성
        new File(OUTPUT_DIR).mkdirs();

        try {
            if (args.length == 0) {
                // 샘플 이미지로 테스트
                runSampleTest();
            } else {
                // 입력 이미지로 테스트
                runTestWithImages(args);
            }

            System.out.println("\n✅ 모든 테스트 완료!");
            System.out.println("📁 결과 파일 위치: " + new File(OUTPUT_DIR).getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ 에러 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 샘플 이미지로 테스트
     */
    private static void runSampleTest() throws Exception {
        System.out.println("📌 샘플 이미지로 테스트 실행\n");

        // 샘플 이미지 생성
        BufferedImage sample1 = createSampleImage(800, 600, new Color(70, 130, 180), "Image 1");
        BufferedImage sample2 = createSampleImage(600, 800, new Color(60, 179, 113), "Image 2");
        BufferedImage sample3 = createSampleImage(700, 700, new Color(255, 165, 0), "Image 3");
        BufferedImage sample4 = createSampleImage(900, 600, new Color(147, 112, 219), "Image 4");

        // 1. 기본 카드 이미지 테스트
        System.out.println("1️⃣ 기본 카드 이미지 생성...");
        BufferedImage basicCard = ImageUtil.createCardImageWithFixedCanvas(sample1);
        ImageUtil.saveImage(basicCard, OUTPUT_DIR + "/1_basic_card.png");

        // 2. 프리미엄 카드 이미지 테스트
        System.out.println("2️⃣ 프리미엄 카드 이미지 생성 (고급 그림자)...");
        BufferedImage premiumCard = ImageUtil.createPremiumCardImage(sample1);
        ImageUtil.saveImage(premiumCard, OUTPUT_DIR + "/2_premium_card.png");

        // 3. 2열 그리드 테스트
        System.out.println("3️⃣ 2열 그리드 이미지 생성 (양옆 배치)...");
        BufferedImage twoColumn = ImageUtil.createTwoColumnGrid(sample1, sample2);
        ImageUtil.saveImage(twoColumn, OUTPUT_DIR + "/3_two_column_grid.png");

        // 4. 2x2 그리드 테스트
        System.out.println("4️⃣ 2x2 그리드 이미지 생성...");
        BufferedImage twoByTwo = ImageUtil.createTwoByTwoGrid(sample1, sample2, sample3, sample4);
        ImageUtil.saveImage(twoByTwo, OUTPUT_DIR + "/4_two_by_two_grid.png");

        // 5. 동적 그리드 테스트
        System.out.println("5️⃣ 동적 그리드 이미지 생성 (5개 이미지)...");
        List<BufferedImage> images = new ArrayList<>();
        images.add(sample1);
        images.add(sample2);
        images.add(sample3);
        images.add(sample4);
        images.add(createSampleImage(800, 500, new Color(220, 20, 60), "Image 5"));

        List<BufferedImage> grids = ImageUtil.createMultipleGridImages(images);
        for (int i = 0; i < grids.size(); i++) {
            ImageUtil.saveImage(grids.get(i), OUTPUT_DIR + "/5_dynamic_grid_" + (i + 1) + ".png");
        }

        // 비교 이미지 생성
        System.out.println("\n6️⃣ 기본 vs 프리미엄 비교 이미지 생성...");
        BufferedImage comparison = createComparisonImage(basicCard, premiumCard);
        ImageUtil.saveImage(comparison, OUTPUT_DIR + "/6_comparison_basic_vs_premium.png");
    }

    /**
     * 입력된 이미지로 테스트
     */
    private static void runTestWithImages(String[] imagePaths) throws Exception {
        System.out.println("📌 입력 이미지로 테스트 실행\n");

        List<BufferedImage> images = new ArrayList<>();
        for (String path : imagePaths) {
            System.out.println("📷 이미지 로드: " + path);
            try {
                BufferedImage img = ImageUtil.loadImageWithOrientation(path);
                images.add(img);
            } catch (Exception e) {
                // EXIF 처리 실패 시 일반 로드
                BufferedImage img = ImageUtil.loadImage(path);
                images.add(img);
            }
        }

        if (images.size() == 1) {
            // 단일 이미지
            System.out.println("\n1️⃣ 프리미엄 카드 이미지 생성...");
            BufferedImage premium = ImageUtil.createPremiumCardImage(images.get(0));
            ImageUtil.saveImage(premium, OUTPUT_DIR + "/result_premium_card.png");

            System.out.println("2️⃣ 기본 카드 이미지 생성...");
            BufferedImage basic = ImageUtil.createCardImageWithFixedCanvas(images.get(0));
            ImageUtil.saveImage(basic, OUTPUT_DIR + "/result_basic_card.png");

        } else if (images.size() == 2) {
            // 2개 이미지 - 양옆 배치
            System.out.println("\n1️⃣ 2열 그리드 이미지 생성...");
            BufferedImage grid = ImageUtil.createTwoColumnGrid(images.get(0), images.get(1));
            ImageUtil.saveImage(grid, OUTPUT_DIR + "/result_two_column.png");

        } else if (images.size() >= 4) {
            // 4개 이미지 - 2x2 그리드
            System.out.println("\n1️⃣ 2x2 그리드 이미지 생성...");
            BufferedImage grid = ImageUtil.createTwoByTwoGrid(
                images.get(0), images.get(1), images.get(2), images.get(3));
            ImageUtil.saveImage(grid, OUTPUT_DIR + "/result_2x2_grid.png");
        }

        // 동적 그리드도 생성
        System.out.println("\n📊 동적 그리드 이미지 생성...");
        List<BufferedImage> grids = ImageUtil.createMultipleGridImages(images);
        for (int i = 0; i < grids.size(); i++) {
            ImageUtil.saveImage(grids.get(i), OUTPUT_DIR + "/result_grid_" + (i + 1) + ".png");
        }
    }

    /**
     * 샘플 이미지 생성
     */
    private static BufferedImage createSampleImage(int width, int height, Color color, String text) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 그라데이션 배경
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / height;
            int r = (int) (color.getRed() * (1 - ratio * 0.3));
            int gr = (int) (color.getGreen() * (1 - ratio * 0.3));
            int b = (int) (color.getBlue() * (1 - ratio * 0.3));
            g.setColor(new Color(Math.max(0, r), Math.max(0, gr), Math.max(0, b)));
            g.drawLine(0, y, width, y);
        }

        // 텍스트
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(48f));
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (width - textWidth) / 2, height / 2);

        // 크기 표시
        g.setFont(g.getFont().deriveFont(24f));
        String sizeText = width + " x " + height;
        int sizeWidth = g.getFontMetrics().stringWidth(sizeText);
        g.drawString(sizeText, (width - sizeWidth) / 2, height / 2 + 40);

        g.dispose();
        return image;
    }

    /**
     * 비교 이미지 생성
     */
    private static BufferedImage createComparisonImage(BufferedImage img1, BufferedImage img2) {
        int width = img1.getWidth() + img2.getWidth() + 40;
        int height = Math.max(img1.getHeight(), img2.getHeight()) + 80;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // 제목
        g.setColor(Color.DARK_GRAY);
        g.setFont(g.getFont().deriveFont(24f));
        g.drawString("기본 (Basic)", 20, 35);
        g.drawString("프리미엄 (Premium)", img1.getWidth() + 40, 35);

        // 이미지
        g.drawImage(img1, 10, 50, null);
        g.drawImage(img2, img1.getWidth() + 30, 50, null);

        g.dispose();
        return result;
    }
}
