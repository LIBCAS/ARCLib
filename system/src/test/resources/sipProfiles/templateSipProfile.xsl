<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:METS="http://www.loc.gov/METS/"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:v3="http://www.loc.gov/mods/v3"
                xmlns:ARCLib="http://arclib.lib.cas.cz/ARCLIB_XSD"
                xmlns:premis="info:lc/xmlns/premis-v2"
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

    <!-- == CUSTOM TEMPLATES == -->

    <!--Copy node at XPath specified with the 'select' to the XPath specified with the 'match'-->
    <xsl:template match="/METS:mets/METS:dmdSec/METS:mdWrap/METS:xmlData/oai_dc:dc">
        <xsl:copy>
            <xsl:copy-of
                    select="/METS:mets/METS:dmdSec/METS:mdWrap/METS:xmlData/v3:mods/v3:titleInfo/v3:title"/>

            <xsl:call-template name="copy-children"/>
        </xsl:copy>
    </xsl:template>

    <!--Copy nodes from multiple files specified by regex-->
    <xsl:template match="/METS:mets/METS:metsHdr">
        <xsl:copy>
            <xsl:element name="ARCLIB:formats">
                <xsl:for-each
                        select="collection($amdSecs)">
                    <ARCLib:format>
                        <xsl:value-of
                                select="/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/METS:xmlData/premis:object/
                        premis:objectCharacteristics/premis:format/premis:formatDesignation/premis:formatName"/>
                    </ARCLib:format>
                </xsl:for-each>
            </xsl:element>
            <xsl:call-template name="copy-children"/>
        </xsl:copy>
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
