package com.orientdb.samples.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.orientdb.executor.OGremlinResult;
import org.apache.tinkerpop.gremlin.orientdb.executor.OGremlinResultSet;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TransactionTest {
    private static final String BELONGS_TO = "BELONGS_TO";
    private static OrientGraphFactory factory = null;
    public static final String EMPLOYEE = "Employee";
    public static final String NAME = "name";
    public static final String STREET = "street";

    @BeforeTest
    public static OrientGraphFactory setup() {
        factory = new OrientGraphFactory("memory:trans", "admin", "admin").setupPool(1, 10);
        OrientGraph graph = factory.getNoTx();
        setupDbSchema(graph);
        return factory;
    }

    @AfterTest
    public static void cleanUp() {

    }


    @Test(priority = 2)
    public void testDbTransactionOperations_Not_Working() {
        // Block for creating employees
        OrientGraph graph1 = factory.getTx();
        OrientGraph graph2 = factory.getTx();
        try {
            // Creating employees
            Vertex empVertex1 = graph1.addVertex(T.label, EMPLOYEE, NAME, "first +  last" + new Random().nextDouble(),
                    STREET, "Street" + new Random().nextDouble());
            Vertex empVertex2 = graph1.addVertex(T.label, EMPLOYEE, NAME, "first +  last" + new Random().nextDouble(),
                    STREET, "Street" + new Random().nextDouble());
            Vertex empVertex3 = graph1.addVertex(T.label, EMPLOYEE, NAME, "first +  last" + new Random().nextDouble(),
                    STREET, "Street" + new Random().nextDouble());
            Vertex empVertex4 = graph1.addVertex(T.label, EMPLOYEE, NAME, "first +  last" + new Random().nextDouble(),
                    STREET, "Street" + new Random().nextDouble());



            graph1.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != graph1 && !graph1.isClosed()) {
                graph1.close();
            }
        }

        // Block for testing the created employees count
        int count = 0;
        try {
            OGremlinResultSet vertices = graph2.executeSql("select from Employee");
            Iterator<OGremlinResult> iterator = vertices.iterator();
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != graph2 && !graph2.isClosed()) {
                graph2.close();
            }
        }

        Assert.assertEquals(count, 4, "Transaction is not rolleed back, records created");

    }

    @Test(priority = 2)
    public void testDbTransactionOperations_new_bug() {
        // Block for creating employees
        OrientGraph graph1 = factory.getTx();
        OrientGraph graph2 = factory.getTx();
        try {
            // Creating employees
            Vertex empVertex1 = graph1.addVertex(T.label, EMPLOYEE, NAME, "first +  last" + new Random().nextDouble(),
                    STREET, "Street" + new Random().nextDouble());

            // Invalid country vertex
            Vertex countryVertex = null;
            OGremlinResultSet vertices = graph2.executeSql("select from Country where id  = ? ", 44);

            Iterator<OGremlinResult> iterator = vertices.iterator();
            countryVertex = iterator.hasNext() ? iterator.next().getVertex().get() : null;

            empVertex1.addEdge(BELONGS_TO, countryVertex);

            graph1.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != graph1 && !graph1.isClosed()) {
                graph1.close();
            }
        }

        // Block for testing the created employees count
        int count = 0;
        try {
            OGremlinResultSet vertices = graph2.executeSql("select from Employee");
            Iterator<OGremlinResult> iterator = vertices.iterator();
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != graph2 && !graph2.isClosed()) {
                graph2.close();
            }
        }

        Assert.assertEquals(count, 0, "Transaction is not rolleed back, records created");

    }

    @Test(priority = 1)
    public void testDbTransactionOperations_Working() {
        // Block for creating employees
        OrientGraph noTxGraph = factory.getNoTx();
        OrientGraph graph = factory.getTx();
        try {
            noTxGraph = factory.getNoTx();
            // Creating employees
            noTxGraph.begin();
            Vertex empVertex1 =
                    noTxGraph.addVertex(T.label, EMPLOYEE, NAME, "first +  last" + new Random().nextDouble());

            noTxGraph.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != noTxGraph && !noTxGraph.isClosed()) {
                noTxGraph.close();
            }
        }

        // Block for testing the created employees count
        int count = 0;
        try {
            OGremlinResultSet vertices = graph.executeSql("select from Employee");
            Iterator<OGremlinResult> iterator = vertices.iterator();
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != graph && !graph.isClosed()) {
                graph.close();
            }
        }

        Assert.assertEquals(count, 0, "Transaction is not rolleed back, records created");

    }

    private static void setupDbSchema(OrientGraph noTxGraph) {
        Map<String, Object> params = new HashMap<String, Object>();

        noTxGraph.executeSql("CREATE CLASS Employee EXTENDS V", params);
        noTxGraph.executeSql("CREATE SEQUENCE employeeIdSequence TYPE ORDERED;", params);
        noTxGraph.executeSql(
                "CREATE PROPERTY Employee.id LONG (MANDATORY TRUE, default \"sequence('employeeIdSequence').next()\");",
                params);
        noTxGraph.executeSql("CREATE INDEX Employee.id ON Employee (id) UNIQUE");
        noTxGraph.executeSql("CREATE PROPERTY Employee.name STRING (MANDATORY TRUE, MIN 4, MAX 50);", params);
        noTxGraph.executeSql("CREATE PROPERTY Employee.street STRING (MANDATORY TRUE)", params);

        noTxGraph.executeSql("CREATE CLASS Country EXTENDS V", params);
        noTxGraph.executeSql("CREATE SEQUENCE countryIdSequence TYPE ORDERED;", params);
        noTxGraph.executeSql(
                "CREATE PROPERTY Country.id LONG (MANDATORY TRUE, default \"sequence('countryIdSequence').next()\");",
                params);
        noTxGraph.executeSql("CREATE INDEX Country.id ON Country (id) UNIQUE");
        noTxGraph.executeSql("CREATE PROPERTY Country.name STRING (MANDATORY TRUE, MIN 4, MAX 50);", params);

        noTxGraph.executeSql("CREATE VERTEX ", params);
    }

    private static void cleanUpDBSchema(OrientGraph noTxGraph) {
        noTxGraph.executeSql("DELETE class V ;");
    }

}
