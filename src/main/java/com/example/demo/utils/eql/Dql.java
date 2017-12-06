package com.example.demo.utils.eql;

import org.n3r.eql.Eql;
import org.n3r.eql.config.EqlConfig;
import org.n3r.eql.config.EqlDiamondConfig;

/**
 * Created by beck on 2017/11/20.
 */
public class Dql extends Eql {
    public Dql() {
        super(createEqlConfig(), Eql.STACKTRACE_DEEP_FIVE);
    }

    public Dql(String connectionName) {
        super(createEqlConfig(connectionName), Eql.STACKTRACE_DEEP_FIVE);
    }


    public static EqlConfig createEqlConfig() {
        return createEqlConfig("free");
    }


    public static EqlConfig createEqlConfig(String connectionName) {
        return new EqlDiamondConfig(connectionName);
    }
}
