<p>Hello,</p>

<p>in ${createdDate} at ${createdTime} reports have been generated.</p>
<p>${result}</p>
<div>
    <#list reports>
        <ul>
            <#items as report>
                <li>${report}</li>
            </#items>
        </ul>
    </#list>
</div>

<p>
    Best regards,
    <br/>
    ${appName} application
</p>

<p style="color: silver">This is an automated message, please do not reply.</p>
