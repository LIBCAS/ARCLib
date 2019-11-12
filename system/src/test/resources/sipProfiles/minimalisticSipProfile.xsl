<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
    <!-- == SPECIFICATION OF INPUT FILES == -->

    <!--Path to SIP passed from the Java code-->
    <xsl:param name="pathToSip"/>

    <!--Path to main XML record of SIP passed from the Java code-->
    <xsl:param name="sipMetadataPath"/>

    <xsl:variable name="sipMetadata" select="document(concat('file:///', $pathToSip, $sipMetadataPath))/*"/>

    <!--Collection of documents-->
    <xsl:variable name="amdSecs" select="concat('file:///', $pathToSip, '/amdSec?select=AMD*.xml;recurse=no')"/>

    <!-- == SETTING THE DEFAULT DOCUMENT == -->

    <xsl:template match="/">
        <xsl:apply-templates select="$sipMetadata"/>
    </xsl:template>

    <!-- == GENERIC TEMPLATES: == -->

    <!-- Copy the children of the current node. -->
    <xsl:template name="copy-children">
        <xsl:copy-of select="./*"/>
    </xsl:template>

    <!-- Generic identity template -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
