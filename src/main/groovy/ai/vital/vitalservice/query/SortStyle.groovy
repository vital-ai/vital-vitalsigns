package ai.vital.vitalservice.query

public enum SortStyle {

    /**
     * default sort style which is sql-like. The 1st column value is used for comparison with other match. If values are different the comparison is complete, otherwise the 2nd column is used and so on.
     */
    inOrder,

    /**
     * the sort column values are iterated until non-null values is spotted. It is then used for comparison with other match.
     * It is necessary that all column types are compatible: strings vs numbers. First sort property determines the sort order      
     */
    merged
    
    
    
}
