package org.artifactory.storage.db.aql.service;

import org.apache.commons.io.IOUtils;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.AqlJsonStreamer;
import org.artifactory.aql.result.AqlLazyResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author gidis
 */
public class AqlBuildPromotionOrientedTest extends AqlAbstractServiceTest {
    @Autowired
    private AqlServiceImpl aqlService;

    @Test
    public void itemFilteringByPromotion() {
        AqlEagerResult results = aqlService.executeQueryEager(
                "items.find({\"artifact.module.build.promotion.user\":{\"$eq\": \"me\"}})"
        );

        List items = results.getResults();
        assertEquals(items.size(), 0);
    }

    @Test
    public void promotion() {
        AqlEagerResult results = aqlService.executeQueryEager(
                "build.promotions.find({\"user\":{\"$eq\": \"me\"}})"
        );

        List promotions = results.getResults();
        assertEquals(promotions.size(), 1);
        assertBuildPromotion(results, "promoter", "me");
    }

    @Test
    public void promotionWithLazy() {
        AqlLazyResult aqlLazyResult = aqlService.executeQueryLazy(
                "build.promotions.find({\"user\":{\"$eq\": \"me\"}})"
        );
        AqlJsonStreamer streamer = new AqlJsonStreamer(aqlLazyResult);
        byte[] array;
        try {
            array = streamer.read();
            StringBuilder builder=new StringBuilder();
            try {
                while (array != null) {
                    builder.append(new String(array));
                    array = streamer.read();
                }
                String result = builder.toString();
                Assert.assertTrue(result.contains("\"build.promotion.comment\" : \"sending to QA\""));
                Assert.assertTrue(result.contains("\"build.promotion.created_by\" : \"promoter\""));
                Assert.assertTrue(result.contains("\"build.promotion.repo\" : \"qa-local\""));
                Assert.assertTrue(result.contains("\"build.promotion.status\" : \"promoted\""));
                Assert.assertTrue(result.contains("\"build.promotion.user\" : \"me\""));
            } finally {
                IOUtils.closeQuietly(streamer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void buldWithPromotionWithLazy() {
        AqlLazyResult aqlLazyResult = aqlService.executeQueryLazy(
                "builds.find({\"number\":{\"$eq\": \"2\"}}).include(\"promotion\")"
        );
        AqlJsonStreamer streamer = new AqlJsonStreamer(aqlLazyResult);
        byte[] array;
        try {
            array = streamer.read();
            StringBuilder builder=new StringBuilder();
            try {
                while (array != null) {
                    builder.append(new String(array));
                    array = streamer.read();
                }
                String result = builder.toString();
                Assert.assertTrue(result.contains("\"build.promotion.status\" : \"promoted\""));
                Assert.assertTrue(result.contains("\"build.promotion.created_by\" : \"tester\""));
                Assert.assertTrue(result.contains("\"build.name\" : \"bb\""));
                Assert.assertTrue(result.contains("\"build.promotion.status\" : \"rollback\""));
            } finally {
                IOUtils.closeQuietly(streamer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
