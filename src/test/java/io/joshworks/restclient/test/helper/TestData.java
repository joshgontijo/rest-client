package io.joshworks.restclient.test.helper;

public class TestData {

    private String someField;

    public TestData() {
    }

    public TestData(String someField) {
        this.someField = someField;
    }

    public String getSomeField() {
        return someField;
    }

    public void setSomeField(String someField) {
        this.someField = someField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestData that = (TestData) o;

        return someField != null ? someField.equals(that.someField) : that.someField == null;
    }

    @Override
    public int hashCode() {
        return someField != null ? someField.hashCode() : 0;
    }
}
