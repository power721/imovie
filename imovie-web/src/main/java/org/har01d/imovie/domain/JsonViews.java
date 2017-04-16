package org.har01d.imovie.domain;

public class JsonViews {

    public interface Public {

    }

    public interface Internal extends Public {

    }

    public interface Secret extends Internal {

    }

    public interface List extends Public {

    }

    public interface Detail extends List {

    }
}
