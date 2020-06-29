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
                                        group-by="concat(premis:objectCharacteristics/premis:format,
                                                     premis:objectCharacteristics/premis:creatingApplication/
                                                     premis:creatingApplicationName,
                                                     premis:objectCharacteristics/premis:creatingApplication/
                                                     premis:creatingApplicationVersion,
                                                     substring(premis:objectCharacteristics/premis:creatingApplication/
                                                     premis:dateCreatedByApplication, 1, 10))">
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
                                        <xsl:variable
                                                name="scannerModelSerialNo"
                                                select="/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/
                                                        METS:xmlData/mix:mix[mix:BasicDigitalObjectInformation/
                                                        mix:ObjectIdentifier/mix:objectIdentifierValue/text()=
                                                        current()/premis:objectIdentifier/premis:objectIdentifierValue/text()]
                                                        /mix:ImageCaptureMetadata/mix:ScannerCapture/mix:ScannerModel/
                                                        mix:scannerModelSerialNo"
                                        />
                                        <xsl:if test="$scannerModelSerialNo">
                                            <xsl:element name="ARCLib:scannerModelSerialNo">
                                                <xsl:value-of
                                                        select="$scannerModelSerialNo"/>
                                            </xsl:element>
                                        </xsl:if>
                                        <xsl:element name="ARCLib:fileCount">
                                            <xsl:value-of select="count(current-group())"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:size">
                                            <xsl:value-of select="xs:decimal(sum(current-group()/premis:objectCharacteristics/premis:size/text()))"/>
                                        </xsl:element>
                                    </xsl:element>
                                </xsl:for-each-group>
                            </xsl:element>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <!--generate ARCLib:devices-->
            <xsl:element name="METS:amdSec">
                <xsl:element name="METS:techMD">
                    <xsl:attribute name="ID">ARCLIB_002</xsl:attribute>
                    <xsl:element name="METS:mdWrap">
                        <xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
                        <xsl:element name="METS:xmlData">
                            <xsl:element name="ARCLib:devices">
                                <xsl:for-each-group
                                        select="$amdSecs/METS:mets/METS:amdSec/METS:digiprovMD/METS:mdWrap/
                                    METS:xmlData/premis:event"
                                        group-by="premis:linkingAgentIdentifier/premis:linkingAgentIdentifierValue">
                                    <xsl:element name="ARCLib:device">
                                        <xsl:element name="ARCLib:deviceId">
                                            <xsl:value-of select="current-grouping-key()"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:fileCount">
                                            <xsl:value-of select="count(current-group())"/>
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
                    <xsl:attribute name="ID">ARCLIB_003</xsl:attribute>
                    <xsl:element name="METS:mdWrap">
                        <xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
                        <xsl:element name="METS:xmlData">
                            <xsl:element name="ARCLib:eventAgents">
                                <xsl:for-each-group
                                        select="$amdSecs/METS:mets/METS:amdSec/METS:digiprovMD/
                                        METS:mdWrap/METS:xmlData/premis:event"
                                        group-by="concat(premis:eventType,
                                                    premis:linkingAgentIdentifier/premis:linkingAgentIdentifierValue,
                                                    substring(premis:eventDateTime, 1, 10))">
                                    <xsl:element name="ARCLib:eventAgent">
                                        <xsl:element name="ARCLib:eventType">
                                            <xsl:value-of
                                                    select="premis:eventType"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:agentName">
                                            <xsl:value-of
                                                    select="distinct-values(/METS:mets/METS:amdSec/METS:digiprovMD/METS:mdWrap/
                                                    METS:xmlData/premis:agent[premis:agentIdentifier/
                                                    premis:agentIdentifierValue/text() =
                                                    current()/premis:linkingAgentIdentifier/
                                                    premis:linkingAgentIdentifierValue/text()]/premis:agentName)"/>
                                        </xsl:element>
                                        <xsl:element name="ARCLib:linkingDeviceID">
                                            <xsl:value-of
                                                    select="premis:linkingAgentIdentifier/premis:linkingAgentIdentifierValue"/>
                                        </xsl:element>
                                        <xsl:variable
                                                name="scannerModelSerialNo"
                                                select="/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/METS:xmlData/
                                                mix:mix[mix:BasicDigitalObjectInformation/mix:ObjectIdentifier/
                                                mix:objectIdentifierValue = current()/premis:linkingObjectIdentifier/
                                                premis:linkingObjectIdentifierValue]/mix:ImageCaptureMetadata/
                                                mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo"
                                        />
                                        <xsl:if test="$scannerModelSerialNo">
                                            <xsl:element name="ARCLib:scannerModelSerialNo">
                                                <xsl:value-of select="$scannerModelSerialNo"/>
                                            </xsl:element>
                                        </xsl:if>
                                        <xsl:variable
                                                name="scanningSoftwareName"
                                                select="/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/METS:xmlData/
                                                mix:mix[mix:BasicDigitalObjectInformation/mix:ObjectIdentifier/
                                                mix:objectIdentifierValue = current()/premis:linkingObjectIdentifier/
                                                premis:linkingObjectIdentifierValue]/mix:ImageCaptureMetadata/
                                                mix:ScannerCapture/mix:ScanningSystemSoftware/mix:scanningSoftwareName"
                                        />
                                        <xsl:if test="$scanningSoftwareName">
                                            <xsl:element name="ARCLib:scanningSoftwareName">
                                                <xsl:value-of select="$scanningSoftwareName"/>
                                            </xsl:element>
                                        </xsl:if>
                                        <xsl:element name="ARCLib:eventDate">
                                            <xsl:value-of
                                                    select="substring(premis:eventDateTime, 1, 10)"/>
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
            <!--generate ARCLib:ImageCaptureMetadata-->
            <xsl:element name="METS:amdSec">
                <xsl:element name="METS:techMD">
                    <xsl:attribute name="ID">ARCLIB_004</xsl:attribute>
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
                    <xsl:attribute name="ID">ARCLIB_005</xsl:attribute>
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
