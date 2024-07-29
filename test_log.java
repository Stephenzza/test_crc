public class MultiLineLogExample {

    private static final Logger logger = LoggerFactory.getLogger(MultiLineLogExample.class);

    public static void main(String[] args) {
        MultiLineLogExample example = new MultiLineLogExample();
        example.processData();
    }

    public void processData() {
        logger.info("Start processing data.");

        // Simulating reading data from a source
        logger.info("Reading data line 1");
        logger.info("Reading data line 2");
        logger.info("Reading data line 3");

        // Simulating data transformation
        logger.info("Transforming data block 1");
        logger.info("Transforming data block 2");
        logger.info("Transforming data block 3");

        logger.info("Finished processing data.");
    }
}
