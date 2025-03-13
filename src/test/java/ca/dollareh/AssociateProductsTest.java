package ca.dollareh;

import ca.dollareh.integration.Shopify;
import ca.dollareh.vendor.MultiCraft;
import ca.dollareh.vendor.ProductSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

public class AssociateProductsTest {

    final File rootFolder = new File("workspace/export/Shopify/MultiCraft");

    final Logger logger = LoggerFactory.getLogger(AssociateProductsTest.class);

    @Test
    void testBuildAssociations() throws IOException, InterruptedException, URISyntaxException {
        ProductSource productSource = ProductSource
                .from(MultiCraft.class)
                .onNew(newProduct -> {
                    logger.info("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    logger.info("Product Modified " + updatedProduct);
                })
                .build();

        Shopify shopify = new Shopify(productSource);

        Map<Long, Map<String, Object>> cMap = shopify.getShopifyCollection();

        Map<String, Long> colleionManeMap = new HashMap<>();

        cMap.entrySet().forEach(longMapEntry -> {
            colleionManeMap.put((String) longMapEntry.getValue().get("title"),longMapEntry.getKey() );
        });

        colleionManeMap.put("Paint Brushes & Sponges",329286221994L);
        colleionManeMap.put("Paint Brushes",329284354218L);
        colleionManeMap.put("Glass Beads",329287368874L);
        colleionManeMap.put("Pony Beads",329285304490L);
        colleionManeMap.put("Pearl Beads",329282715818L);
        colleionManeMap.put("Globe Beads",329282912426L);
        colleionManeMap.put("Fashion Beads",329280454826L);
        colleionManeMap.put("Metal Effect Beads",329283666090L);
        colleionManeMap.put("Glass Bead Kits",329286746282L);
        colleionManeMap.put("Wodden Bead Kits",329283829930L);
        colleionManeMap.put("Wires & Cords",329281732778L);
        colleionManeMap.put("Gift Bags & Favor Boxes",329284976810L);
        colleionManeMap.put("Label Tags",329283731626L);
        colleionManeMap.put("Tools",329285337258L);
        colleionManeMap.put("Rattail Cord",329287139498L);
        colleionManeMap.put("Suede",329284386986L);
        colleionManeMap.put("Leatherette",329286615210L);
        colleionManeMap.put("Waxed Cord",329286779050L);
        colleionManeMap.put("Hemp Cord",329280749738L);
        colleionManeMap.put("Goody Bags",329284157610L);
        colleionManeMap.put("Ornaments",329285992618L);
        colleionManeMap.put("Florals",329280553130L);
        colleionManeMap.put("Pens, Pencils & Markers",329283174570L);
        colleionManeMap.put("Glass Markers",329285697706L);
        colleionManeMap.put("Nautical Ropes",329286844586L);
        colleionManeMap.put("Wreaths",329282781354L);
        colleionManeMap.put("Nests",329281470634L);
        colleionManeMap.put("Ice Cubes",329280487594L);
        colleionManeMap.put("Signs",329283436714L);
        colleionManeMap.put("Stickers",329286680746L);
        colleionManeMap.put("Paints",329287401642L);
        colleionManeMap.put("Kids Craft",329286713514L);
        colleionManeMap.put("Mosaic Art",329280225450L);
        colleionManeMap.put("Wooden Kits",329285632170L);
        colleionManeMap.put("Peel-n-Stick Foam Kits",329282453674L);
        colleionManeMap.put("Suncatcher Kits",329281568938L);
        colleionManeMap.put("Primed Panels",329284747434L);
        colleionManeMap.put("Printed Coloring Canvases",329284419754L);
        colleionManeMap.put("Stamping & Card Making",329285435562L);
        colleionManeMap.put("Stamp Applicators",329284878506L);
        colleionManeMap.put("Tool Kits",329283600554L);
        colleionManeMap.put("Scissors",329283535018L);
        colleionManeMap.put("Hammers",329280651434L);
        colleionManeMap.put("PVC Coated Wires",329284649130L);
        colleionManeMap.put("Wood Décor",329283076266L);
        colleionManeMap.put("Letters & Numbers",329285140650L);
        colleionManeMap.put("Wodden Beads",329285861546L);
        colleionManeMap.put("Spools",329285238954L);
        colleionManeMap.put("Sticks",329280684202L);
        colleionManeMap.put("Wheels",329282748586L);
        colleionManeMap.put("Flower Pots",329284714666L);
        colleionManeMap.put("Rings",329281994922L);
        colleionManeMap.put("Slats",329284780202L);
        colleionManeMap.put("Ladders",329287434410L);
        colleionManeMap.put("Fence",329280258218L);
        colleionManeMap.put("Slices",329281536170L);
        colleionManeMap.put("Plaques",329283207338L);
        colleionManeMap.put("Googly Eyes",329281077418L);
        colleionManeMap.put("Floral Wires",329283371178L);
        colleionManeMap.put("Feathers",329750544554L);
        colleionManeMap.put("",329733898410L);
        colleionManeMap.put("Shears",329280979114L);
        colleionManeMap.put("Jute Cords",329286287530L);
        colleionManeMap.put("Tombstone Saddle Iron",329286877354L);
        colleionManeMap.put("Sponges",329282289834L);
        colleionManeMap.put("Storage & Organizers",329285796010L);
        colleionManeMap.put("Mini Glass Bottles",329281503402L);
        colleionManeMap.put("Papers, Pads & Cardstocks",329283141802L);
        colleionManeMap.put("Stack Pads",329286418602L);
        colleionManeMap.put("Foam & Felts",329280848042L);
        colleionManeMap.put("Foam Sheets",329284681898L);
        colleionManeMap.put("Felt Sheets",329280127146L);
        colleionManeMap.put("Glitters & Confettis",329287172266L);
        colleionManeMap.put("Glitters",329284616362L);
        colleionManeMap.put("Glue Guns",329284944042L);
        colleionManeMap.put("Sticky Tacks",329282420906L);
        colleionManeMap.put("Sealer",329283010730L);
        colleionManeMap.put("Primer & Surface Prep",329286582442L);
        colleionManeMap.put("Heavy Gel",329280913578L);
        colleionManeMap.put("Texture Paste",329284452522L);
        colleionManeMap.put("Glues & Podges",329286320298L);
        colleionManeMap.put("Glue Sticks",329282322602L);
        colleionManeMap.put("Glow in the Dark",329281798314L);
        colleionManeMap.put("Gel Pens",329280618666L);
        colleionManeMap.put("Glitter Paint",329281110186L);
        colleionManeMap.put("Metallic Brush Marker",329286353066L);
        colleionManeMap.put("Water Based Paint Marker",329286025386L);
        colleionManeMap.put("Oil Pastels",329285566634L);
        colleionManeMap.put("Water-Based Twin Markers",329286156458L);
        colleionManeMap.put("Gradient Markers",329287106730L);
        colleionManeMap.put("Watercolors",329283895466L);
        colleionManeMap.put("Metallic Paints",329285370026L);
        colleionManeMap.put("Tempera Paints",329283469482L);
        colleionManeMap.put("Patio Paints",329287270570L);
        colleionManeMap.put("Leather Paints",329281601706L);
        colleionManeMap.put("Stains",329286811818L);
        colleionManeMap.put("Pouring Paints",329287205034L);
        colleionManeMap.put("Jewelry Findings",329284190378L);
        colleionManeMap.put("Plastic Lace",329285828778L);
        colleionManeMap.put("Pom-Poms",329281962154L);
        colleionManeMap.put("Printed Canvas Panels",329285664938L);
        colleionManeMap.put("Macramé Cord",329280520362L);
        colleionManeMap.put("Macramé Planter Hanger Kit",329285599402L);
        colleionManeMap.put("Magnets",329281765546L);
        colleionManeMap.put("Peel-n-Stick",329286189226L);
        colleionManeMap.put("Sheets",329282191530L);
        colleionManeMap.put("Hoops",329279897770L);
        colleionManeMap.put("Floss",329281142954L);
        colleionManeMap.put("Fluffy Yarn",329280290986L);
        colleionManeMap.put("Milk Cotton Yarn",329285894314L);
        colleionManeMap.put("Poly Yarn",329281863850L);
        colleionManeMap.put("Poly-Soft Yarn",329282257066L);
        colleionManeMap.put("Plastic Canvas Needles",329282355370L);
        colleionManeMap.put("Plastic Canvas",329282388138L);
        colleionManeMap.put("Paint Pots",329281634474L);
        colleionManeMap.put("Palette Knifes",329282486442L);
        colleionManeMap.put("Sanding Pad",329284223146L);
        colleionManeMap.put("Fine Point Marker",329287237802L);
        colleionManeMap.put("Pencils",329281306794L);
        colleionManeMap.put("Sketch Pads",329281437866L);
        colleionManeMap.put("Tracing Paper",329286123690L);
        colleionManeMap.put("Paper Palette",329284255914L);
        colleionManeMap.put("Watercolor Pads",329283862698L);
        colleionManeMap.put("Paint Palette",329284288682L);
        colleionManeMap.put("Stencil Set",329283109034L);

        colleionManeMap.put("Painting Accessories",329282977962L);
        colleionManeMap.put("Zipper Polybags",329281700010L);
        colleionManeMap.put("Plastic Bottles",329287336106L);
        colleionManeMap.put("Squeeze Bottles",329281831082L);
        colleionManeMap.put("Spray Bottles",329283764394L);
        colleionManeMap.put("Favor Boxes",329281896618L);
        colleionManeMap.put("Organizers",329283338410L);
        colleionManeMap.put("Jars",329287303338L);
        colleionManeMap.put("Refills & Accessories",329281175722L);
        colleionManeMap.put("Photo Corners",329284092074L);
        colleionManeMap.put("Tapes",329286516906L);
        colleionManeMap.put("Gift Bags",329286549674L);
        colleionManeMap.put("Ink Pads",329283043498L);
        colleionManeMap.put("Styrofoam",329285075114L);
        colleionManeMap.put("Foam Balls",329286254762L);
        colleionManeMap.put("Foam Cones",329280323754L);
        colleionManeMap.put("Foam Disc",329280946346L);
        colleionManeMap.put("Gemstones",329284485290L);
        colleionManeMap.put("Flowers",329282814122L);
        colleionManeMap.put("Glitter Die Cuts",329286385834L);
        colleionManeMap.put("Metal Charms",329284124842L);
        colleionManeMap.put("Metallic Foils",329281339562L);
        colleionManeMap.put("Glue Pads",329280716970L);
        colleionManeMap.put("Holographic",329283240106L);
        colleionManeMap.put("Foil Accents",329286090922L);
        colleionManeMap.put("Glitter",329283993770L);
        colleionManeMap.put("Themed",329282125994L);
        colleionManeMap.put("Handmade",329286910122L);
        colleionManeMap.put("Scoring Tools",329287041194L);
        colleionManeMap.put("Paper Trimmer",329285042346L);
        colleionManeMap.put("Precision",329284550826L);
        colleionManeMap.put("Metallic Marker",329286942890L);
        colleionManeMap.put("Signs & Plaques",329282060458L);
        colleionManeMap.put("Metal Rings & Wreath Forms",329287073962L);
        colleionManeMap.put("Photo Frames",329279963306L);
        colleionManeMap.put("Storage Box",329286058154L);

        FileInputStream file = new FileInputStream(Paths.get("sample/Multicraft.xlsx").toFile());
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);




        int i = 0;

        String  productId ;

        String subCategory ;

        String subSubCategory;



        for (Row row : sheet) {
            if (i != 0 && row.getCell(1) != null) {
                String code = row.getCell(1).getStringCellValue().trim();

                if(!code.isEmpty()) {
                    File[] jsonFiles = rootFolder.listFiles((dir, name) -> name.startsWith(code));


                    if (jsonFiles != null && jsonFiles.length == 1) {
                        File shopifyProductFile = jsonFiles[0];
                        productId = shopifyProductFile.getName()
                                .replaceFirst(code +"-","")
                                .replace(".json","");

                        subCategory = row.getCell(4).getStringCellValue().trim();
                        subSubCategory = row.getCell(5).getStringCellValue().trim();

                        Long subCategoryId = colleionManeMap.get(subCategory);

                        if( subCategoryId == null) {
                            subCategoryId = shopify.getShopifyCollection(subCategory);
                            colleionManeMap.put(subCategory, subCategoryId);


                            System.out.println("colleionManeMap.put(\""+subCategory+"\","+subCategoryId+"L);");

                        }

                        Long subSubCategoryId = colleionManeMap.get(subSubCategory);

                        if( subSubCategoryId == null) {
                            subSubCategoryId = shopify.getShopifyCollection(subSubCategory);
                            colleionManeMap.put(subSubCategory, subSubCategoryId);
                            System.out.println("colleionManeMap.put(\""+subSubCategory+"\","+subSubCategoryId+"L);");
                        }

                        shopify.associateCollection(Long.parseLong(productId), subCategoryId.toString());
                        shopify.associateCollection(Long.parseLong(productId), subSubCategoryId.toString());
                        logger.info(productId + "\t\t"
                                + subCategoryId
                                + "\t\t" + colleionManeMap.get(subSubCategory) );
                        Thread.sleep(1000);

                    }


                }



            }
            i++;
        }




    }

}
