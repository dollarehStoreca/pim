package ca.dollareh;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MoveTest {

    public static void main(String[] args) throws IOException {

        List<String> idsTobeMoved = List.of("CD190B","GP092B","WS684","PA230","WS728","SE103J","CK120D","WS482","BD492A","MP104A","WS436","BD490A","SP294G","CS228F","FC208B","WS316","CD980A","KC824","CK120E","NF062B","WS426","SP231I","CK132H","CK500J","CK257C","PC115A-259","CT213","PA479C","CK132F","WP170F","SS058J","WS352","CP620J","SA220A","FE132D","WP160D","GP092C","FE262E","SP241B","SS035C","SS702A","WS346D","CD964A","CW512","SP231J","FE132C","SP221C","GC657-9358","WS630","WS311","CK203E","SS058I","WS346I","CK203F","SP294C","CD953C","CP620B","GC657-6547","WS319","SP231N","CK502A","CK276E","CK500D","WS265","SS060-20","WS346J","CK192-05","GC404D","CT532","SP231L","SE423B","CW748","SP280I","SS060-16","CW333","SP231K","WS682","CK500H","WS340","CK134F","SS066E","WS280","WS264B","SP231M","SE172C","CS228G","WS224K","WS760","WS218B","WS446","PB038A","SP233-08","WP160C","GP092H","GP098F","CS228B","CK142C","FE132A","CK137B","WS286","SE103I","SS493C","CW285","WS224A","BD720C","CW287","PC115B-260","WS474","CW578","SS057G","FE400","SE139G","CD335","SS493A","PB710","GP192E","WS512","WS284","SS030B","CW625B","SP416B","GP098A","PA644I","CK203C","BD492C","WS224E","WS267","SS032B","GC65710416","WS916","WP170C","CK132D","GP095I","CK219O","BD720A","CD951E","FE238A","SP227B","CK203D","WS510","WS458","CW538","CK500F","FL132B","SS276D","SS360C","WS317","SS493E","SS029A","CS228C","SS259A","CK132K","WS475","WS288","WS437","CK500G","WS346H","SP241E","WS378","BD492E","WS287","WS614","SS028D","SS060-22","SS702F","CK500L","BD720E","CW710A","GB850","CK506E","CP620G","BD492D","BD539A","WS634","GC404C","SE227C","CW509B","SP378A","SE422B","SS035D","WS384A","WS346K","SS700F","WS301","WS358E","SE422D","PB714","CK500K","WS633","FE256B","SE423A","CS228K","CP620F","PB436C","GC657-6971","KC151B","GG614","CD710","GP098G","CD951A","SE171B","WS336","CW747","CS006","WS901","WS481","SS060-17","SS035A","PA718E","FE154B","SP280G","GP098E","SE103G","SP294H","WS723","SS030A","MP104B","PA530","SP294E","CK500E","RS210A","CK142B","CP620A","WS480","WS289B","GP534-GS02","PA644H","PA818","KC181D","SP241G","NC446L","GC402B","SE138D","WS502","CK192-06","BT220","SS406F","PA508","WS220J","SS879B","CK134G","WS780","CK500I","WS220B","WS346A","WS488","SS060-23","WS339","PB436B","CD965","SS066D","WS427","WS708","CP620D","CD980D","ST420D","CS228E","PA644E","KC250A","SS034A","WS298","WS459","CP780","WS782","SP221D","SP241A","SP221B","GC657-7684","SE172B","PC113C4852","PA718C","CK142A","WS372L","WS289A","BD720D","WS309","SS174D","GC405B","WS724","BD539C","WS346L","GP092A","SP270D","GP060H","PB434A","CD330A","WS264A","WS338","SP225C","WS264D","SP241F","CP620H","SS222V","CS228I","WS358F","CW670","WS277A","SS700D","WS943","CK136F");

        String filePath = "sample/Multicraft Final.xlsx"; // Your Excel file path
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);

        Sheet sourceSheet = workbook.getSheetAt(0); // Source sheet
        workbook.createSheet();
        Sheet destinationSheet = workbook.getSheetAt(1); // Destination sheet

        int rowNumber=0 , targetRow = 1;

        List<Integer> rowsTobeMoved = new ArrayList<>();

        for (Row row : sourceSheet) {
            if (rowNumber != 0) {

                String code = row.getCell(1).getStringCellValue();

                if (idsTobeMoved.contains(code)) {
                    rowsTobeMoved.add(rowNumber);
                }

            }
            rowNumber++;
        }

        for (int i = 0; i < rowsTobeMoved.toArray().length; i++) {
            moveRow(sourceSheet, destinationSheet, rowsTobeMoved.get(i), targetRow);
            System.out.println("Moved");
            targetRow++;
        }



        // Save the workbook
        FileOutputStream fos = new FileOutputStream(filePath);
        workbook.write(fos);
        fos.close();
        workbook.close();
        fis.close();
        System.out.println("Row copied successfully!");
    }

    private static void moveRow(Sheet sourceSheet, Sheet destinationSheet, int sourceRowNum, int destinationRowNum) {
        Row sourceRow = sourceSheet.getRow(sourceRowNum);
        if (sourceRow == null) return;

        Row destinationRow = destinationSheet.getRow(destinationRowNum);
        if (destinationRow == null) {
            destinationRow = destinationSheet.createRow(destinationRowNum);
        }

        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            Cell sourceCell = sourceRow.getCell(i);
            Cell destinationCell = destinationRow.createCell(i);

            if (sourceCell != null) {
                copyCell(sourceCell, destinationCell, sourceSheet.getWorkbook());
            }
        }

        // Remove the original row from the source sheet
        sourceSheet.removeRow(sourceRow);
    }

    private static void copyCell(Cell sourceCell, Cell destinationCell, Workbook workbook) {
        CellStyle newCellStyle = workbook.createCellStyle();
        newCellStyle.cloneStyleFrom(sourceCell.getCellStyle()); // Copy style
        destinationCell.setCellStyle(newCellStyle);

        switch (sourceCell.getCellType()) {
            case STRING:
                destinationCell.setCellValue(sourceCell.getStringCellValue());
                break;
            case NUMERIC:
                destinationCell.setCellValue(sourceCell.getNumericCellValue());
                break;
            case BOOLEAN:
                destinationCell.setCellValue(sourceCell.getBooleanCellValue());
                break;
            case FORMULA:
                destinationCell.setCellFormula(sourceCell.getCellFormula());
                break;
            case BLANK:
                destinationCell.setBlank();
                break;
            default:
                break;
        }
    }
}
