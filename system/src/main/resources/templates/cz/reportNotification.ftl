<p>Dobrý den,</p>

<p>dne ${createdDate} ve ${createdTime} byli vygenerované reporty.</p>
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
    S pozdravem
    <br/>
    Aplikace ${appName}
</p>

<p style="color: silver">Tento email byl poslán automatizovaně, proto na něj neodpovídejte.</p>
