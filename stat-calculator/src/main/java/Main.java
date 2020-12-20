import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.bson.Document;
import scala.Tuple2;

public class Main {
    public static void main(String[] args) {
        String statType = args[0];
        System.out.println("STAT TYPE: " + statType);

        SparkSession spark = SparkSession
            .builder()
            .appName("hhvacancyanalysis")
            .master("spark://spark-master:7077")
            .config("spark.mongodb.input.uri", "mongodb://mongo/test.vacancy")
            .config("spark.mongodb.output.uri", "mongodb://mongo/test." + statType)
            .config("spark.jars", "/statCount.jar")
            .getOrCreate();
        JavaSparkContext jsc = JavaSparkContext.fromSparkContext(spark.sparkContext());

        JavaMongoRDD<Document> documentJavaMongoRDD = MongoSpark.load(jsc);

        if (documentJavaMongoRDD.isEmpty()) {
            return;
        }

        JavaRDD<Document> rdd;
        switch (statType) {
            case "scheduleWordCount":
                rdd = scheduleStat(documentJavaMongoRDD);
                break;
            case "skillWordCount":
                rdd = skillStat(documentJavaMongoRDD);
                break;
            case "experienceWordCount":
                rdd = experienceStat(documentJavaMongoRDD);
                break;
            default:
                throw new RuntimeException("Wrong stat type");
        }

        MongoSpark.save(rdd);

        jsc.close();
    }

    private static Document getStatDocument(String keyWord, Number count) {
        return Document.parse("{\"keyword\": \"" + keyWord  + "\", \"count\": \"" + count + "\"}");
    }

    private static JavaRDD<Document> experienceStat(JavaMongoRDD<Document> documentJavaMongoRDD) {
        return documentJavaMongoRDD
            .filter(v -> v.get("experience") != null)
            .map(v ->  v.get("experience", Document.class).get("name").toString())
            .mapToPair(s -> new Tuple2<>(s, 1L))
            .reduceByKey(Long::sum)
            .map(e -> getStatDocument(e._1, e._2()));
    }

    private static JavaRDD<Document> skillStat(JavaMongoRDD<Document> documentJavaMongoRDD) {
        Long skillsCount = documentJavaMongoRDD
            .filter(v -> v.get("key_skills") != null)
            .count();

        System.out.println(skillsCount);

        return documentJavaMongoRDD
            .filter(v -> v.get("key_skills") != null)
            .flatMap(v -> v.getList("key_skills", Document.class).iterator())
            .map(v -> v.get("name").toString())
            //.flatMap(s -> Arrays.stream(s.split("[ ]+")).iterator())
            .mapToPair(s -> new Tuple2<>(s, 1L))
            .reduceByKey(Long::sum)
            .map(e -> getStatDocument(e._1(), ((double) e._2() / (double) skillsCount) * 100));
    }

    private static JavaRDD<Document> scheduleStat(JavaMongoRDD<Document> documentJavaMongoRDD) {
        return documentJavaMongoRDD
                .filter(v -> v.get("schedule") != null)
                .map(v ->  v.get("schedule", Document.class).get("name").toString())
                .mapToPair(s -> new Tuple2<>(s, 1L))
                .reduceByKey(Long::sum)
                .map(e -> getStatDocument(e._1, e._2()));
    }
}
