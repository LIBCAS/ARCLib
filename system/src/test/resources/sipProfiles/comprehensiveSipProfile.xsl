<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:METS="http://www.loc.gov/METS/"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:ARCLib="http://arclib.lib.cas.cz/ARCLIB_XSD"
                xmlns:premis="info:lc/xmlns/premis-v2"
                xmlns:mix="http://www.loc.gov/mix/v20"
                xmlns:xlink="http://www.w3.org/1999/xlink"
>
    <!-- == SPECIFICATION OF INPUT FILES == -->
    <!--Path to SIP passed from the Java code (with trailing / (slash))-->
    <xsl:param name="pathToSip"/>
    <!--Path to main XML record (METS in the root folder) of SIP passed from the Java code-->
    <xsl:param name="sipMetadataPath"/>
    <xsl:variable name="sipMetadata" select="document(concat('file:///', $sipMetadataPath))/*"/>
    <!-- == SETTING THE DEFAULT DOCUMENT == -->
    <xsl:template match="/">
        <xsl:apply-templates select="$sipMetadata"/>
    </xsl:template>
    <!-- == CUSTOM TEMPLATES == -->
    <xsl:template match="/METS:mets">
        <xsl:variable name="amdSecs"
                      select="document(//METS:fileGrp[@ID='TECHMDGRP']/METS:file/METS:FLocat/concat('file:///', string-join(tokenize($sipMetadataPath, '/')[position() lt last()], '/'),'/',@xlink:href))"/>
        <xsl:copy>
            <xsl:attribute name="LABEL">
                <xsl:value-of select="@LABEL"/>
            </xsl:attribute>
            <xsl:attribute name="TYPE">
                <xsl:value-of select="@TYPE"/>
            </xsl:attribute>
            <xsl:element name="METS:metsHdr">
                <xsl:copy-of select="/METS:mets/METS:metsHdr/METS:agent"/>
            </xsl:element>
            <xsl:copy-of select="METS:dmdSec"/>
            <!--generate ARCLib:formats-->
            <xsl:element name="METS:amdSec">
                <xsl:element name="METS:techMD">
                    <xsl:attribute name="ID">ARCLIB_001</xsl:attribute>
                    <xsl:element name="METS:mdWrap">
                        <xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
                        <xsl:element name="METS:xmlData">
                            <xsl:element name="ARCLib:formats">
                                <xsl:for-each-group
                                        select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/
                                        METS:xmlData/premis:object"
                                        group-by="concat(
                                        premis:objectCharacteristics/premis:format/
                                                    premis:formatDesignation/premis:formatName,
                                                    premis:objectCharacteristics/premis:format/
                                                    premis:formatDesignation/premis:formatVersion,
                                                    premis:objectCharacteristics/premis:format/
                                                premis:formatRegistry/premis:formatRegistryKey,
                                                premis:objectCharacteristics/premis:format/
                                                premis:formatRegistry/premis:formatRegistryName,
                                                     premis:objectCharacteristics/premis:creatingApplication/
                                                     premis:creatingApplicationName,
                                                     premis:objectCharacteristics/premis:creatingApplication/
                                                     premis:creatingApplicationVersion,
                                                     substring(premis:objectCharacteristics/premis:creatingApplication/
                                                     premis:dateCreatedByApplication, 1, 10),
                                                premis:preservationLevel/premis:preservationLevelValue)">
                                    <xsl:element name="ARCLib:format">
                                        <xsl:element name="ARCLib:fileFormat">
                                            <xsl:value-of
                                                    select="premis:objectCharacteristics/premis:format/
                                                    premis:formatDesignation/premis:formatName"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:formatVersion">
                                            <xsl:value-of
                                                    select="premis:objectCharacteristics/premis:format/
                                                    premis:formatDesignation/premis:formatVersion"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:formatRegistryKey">
                                            <xsl:value-of
                                                    select="premis:objectCharacteristics/premis:format/
                                                premis:formatRegistry/premis:formatRegistryKey"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:formatRegistryName">
                                            <xsl:value-of
                                                    select="premis:objectCharacteristics/premis:format/
                                                premis:formatRegistry/premis:formatRegistryName"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:creatingApplicationName">
                                            <xsl:value-of
                                                    select="premis:objectCharacteristics/premis:creatingApplication/
                                                    premis:creatingApplicationName"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:creatingApplicationVersion">
                                            <xsl:value-of
                                                    select="premis:objectCharacteristics/premis:creatingApplication/
                                                    premis:creatingApplicationVersion"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:dateCreatedByApplication">
                                            <xsl:value-of
                                                    select="substring(premis:objectCharacteristics/
                                                    premis:creatingApplication/premis:dateCreatedByApplication, 1, 10)"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:preservationLevelValue">
                                            <xsl:value-of
                                                    select="premis:preservationLevel/premis:preservationLevelValue"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:fileCount">
                                            <xsl:value-of select="count(current-group())"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:size">
                                            <xsl:value-of
                                                    select="xs:decimal(sum(current-group()/premis:objectCharacteristics/premis:size/text()))"/>
                                        </xsl:element>
                                    </xsl:element>
                                </xsl:for-each-group>
                            </xsl:element>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <!--generate ARCLib:eventAgents-->
            <xsl:element name="METS:amdSec">
                <xsl:element name="METS:techMD">
                    <xsl:attribute name="ID">ARCLIB_002</xsl:attribute>
                    <xsl:element name="METS:mdWrap">
                        <xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
                        <xsl:element name="METS:xmlData">
                            <xsl:element name="ARCLib:eventAgents">
                                <xsl:variable name="joinedEventAgents">
                                    <xsl:call-template name="eventAgentsT">
                                        <xsl:with-param name="amdSecs" select="$amdSecs"/>
                                    </xsl:call-template>
                                </xsl:variable>
                                <xsl:for-each select="$joinedEventAgents/*">
                                    <xsl:sort select="ARCLib:eventDate"/>
                                    <xsl:sort select="ARCLib:agentName"/>
                                    <xsl:copy-of select="."/>
                                </xsl:for-each>
                            </xsl:element>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <!--generate ARCLib:ImageCaptureMetadata-->
            <xsl:element name="METS:amdSec">
                <xsl:element name="METS:techMD">
                    <xsl:attribute name="ID">ARCLIB_003</xsl:attribute>
                    <xsl:element name="METS:mdWrap">
                        <xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
                        <xsl:element name="METS:xmlData">
                            <xsl:element name="ARCLib:ImageCaptureInformation">
                                <xsl:for-each-group
                                        select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/
                                        METS:xmlData/mix:mix/mix:ImageCaptureMetadata"
                                        group-by="concat(substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10),
                                                    mix:GeneralCaptureInformation/mix:imageProducer,
                                                    mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo)">
                                    <xsl:element name="ARCLib:ImageCaptureMetadata">
                                        <xsl:element name="ARCLib:dateCreated">
                                            <xsl:value-of
                                                    select="substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10)"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:imageProducer">
                                            <xsl:value-of
                                                    select="mix:GeneralCaptureInformation/mix:imageProducer"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:scannerModelSerialNo">
                                            <xsl:value-of
                                                    select="mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:eventCount">
                                            <xsl:value-of select="count(current-group())"/>
                                        </xsl:element>
                                    </xsl:element>
                                </xsl:for-each-group>
                            </xsl:element>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <!--generate ARCLib:creatingApplication-->
            <xsl:element name="METS:amdSec">
                <xsl:element name="METS:techMD">
                    <xsl:attribute name="ID">ARCLIB_004</xsl:attribute>
                    <xsl:element name="METS:mdWrap">
                        <xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
                        <xsl:element name="METS:xmlData">
                            <xsl:element name="ARCLib:creatingApplications">
                                <xsl:for-each-group
                                        select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/
                                        METS:xmlData/premis:object"
                                        group-by="concat(premis:objectCharacteristics/premis:creatingApplication/
                                        premis:creatingApplicationName,
                                                    premis:objectCharacteristics/premis:creatingApplication/
                                                    premis:creatingApplicationVersion,
                                                    substring(premis:objectCharacteristics/premis:creatingApplication/
                                                    premis:dateCreatedByApplication, 1, 10))">
                                    <xsl:element name="ARCLib:creatingApplication">
                                        <xsl:element name="ARCLib:creatingApplicationName">
                                            <xsl:value-of
                                                    select="premis:objectCharacteristics/premis:creatingApplication/
                                                    premis:creatingApplicationName"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:creatingApplicationVersion">
                                            <xsl:value-of
                                                    select="premis:objectCharacteristics/premis:creatingApplication/
                                                    premis:creatingApplicationVersion"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:dateCreatedByApplication">
                                            <xsl:value-of
                                                    select="substring(premis:objectCharacteristics/
                                                    premis:creatingApplication/premis:dateCreatedByApplication, 1, 10)"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:eventCount">
                                            <xsl:value-of select="count(current-group())"/>
                                        </xsl:element>
                                    </xsl:element>
                                </xsl:for-each-group>
                            </xsl:element>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="eventAgentsT">
        <xsl:param name="amdSecs"/>
        <xsl:for-each-group select="$amdSecs/METS:mets/METS:amdSec/METS:digiprovMD/METS:mdWrap/
                                                    METS:xmlData/premis:agent"
                            group-by="premis:agentName">
            <xsl:variable name="agentName" select="premis:agentName"/>
            <xsl:variable name="agentId" select="premis:agentIdentifier/premis:agentIdentifierValue"/>
            <xsl:for-each-group select="$amdSecs/METS:mets/METS:amdSec/METS:digiprovMD/METS:mdWrap/
                                                    METS:xmlData/premis:event[premis:linkingAgentIdentifier/
                                                    premis:linkingAgentIdentifierValue/text() =
                                                    $agentId]"
                                group-by="concat(premis:eventType,substring(premis:eventDateTime,1,10))">
                <xsl:element name="ARCLib:eventAgent">
                    <xsl:element name="ARCLib:eventType">
                        <xsl:value-of
                                select="premis:eventType"/>
                    </xsl:element>
                    <xsl:element name="ARCLib:agentName">
                        <xsl:value-of
                                select="$agentName"/>
                    </xsl:element>
                    <xsl:element name="ARCLib:eventDate">
                        <xsl:value-of
                                select="substring(premis:eventDateTime,1,10)"/>
                    </xsl:element>
                    <xsl:element name="ARCLib:fileCount">
                        <xsl:value-of
                                select="count(current-group()/premis:linkingObjectIdentifier)"/>
                    </xsl:element>
                </xsl:element>
            </xsl:for-each-group>
        </xsl:for-each-group>
        <xsl:for-each-group
                select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/
                                        METS:mdWrap/METS:xmlData/mix:mix/mix:ImageCaptureMetadata"
                group-by="concat(mix:ScannerCapture/mix:ScannerModel/mix:scannerModelName,
                                                    mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo,
                                                    substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10))">
            <xsl:element name="ARCLib:eventAgent">
                <xsl:element name="ARCLib:eventType">
                    <xsl:text>capture</xsl:text>
                </xsl:element>
                <xsl:element name="ARCLib:agentName">
                    <xsl:value-of
                            select="mix:ScannerCapture/mix:ScannerModel/mix:scannerModelName"/>
                </xsl:element>
                <xsl:element name="ARCLib:scannerModelSerialNo">
                    <xsl:value-of
                            select="mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo"/>
                </xsl:element>
                <xsl:element name="ARCLib:eventDate">
                    <xsl:value-of
                            select="substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10)"/>
                </xsl:element>
                <xsl:element name="ARCLib:fileCount">
                    <xsl:value-of select="count(current-group())"/>
                </xsl:element>
            </xsl:element>
        </xsl:for-each-group>
        <xsl:for-each-group
                select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/
                                        METS:mdWrap/METS:xmlData/mix:mix/mix:ImageCaptureMetadata"
                group-by="concat(mix:ScannerCapture/mix:ScanningSystemSoftware/mix:scanningSoftwareName,
                                                    substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10))">
            <xsl:element name="ARCLib:eventAgent">
                <xsl:element name="ARCLib:eventType">
                    <xsl:text>capture</xsl:text>
                </xsl:element>
                <xsl:element name="ARCLib:agentName">
                    <xsl:value-of
                            select="mix:ScannerCapture/mix:ScanningSystemSoftware/mix:scanningSoftwareName"/>
                </xsl:element>
                <xsl:element name="ARCLib:eventDate">
                    <xsl:value-of
                            select="substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10)"/>
                </xsl:element>
                <xsl:element name="ARCLib:fileCount">
                    <xsl:value-of select="count(current-group())"/>
                </xsl:element>
            </xsl:element>
        </xsl:for-each-group>
    </xsl:template>

    <!-- == GENERIC TEMPLATES: == -->
    <!-- Copy the children of the current node. -->
    <xsl:template name="copy-children">
        <xsl:copy-of select="./*"/>
    </xsl:template>
    <!-- Generic identity template -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
