using System;
using System.IO.Pipes;
using System.Net.Sockets;
using System.Runtime.Versioning;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

class MyModel
{
    [JsonPropertyName("myProperty")]
    public string MyProperty { get; set; }
}

class IpcWrapper
{
    [JsonPropertyName("data")]
    public string Data { get; set; }
    [JsonPropertyName("hasError")]
    public bool HasError { get; set; }
    [JsonPropertyName("error")]
    public string Error { get; set; }
}

internal class Program
{
    public static string TransformToXmlTcpSocket(MyModel myModel)
    {
        var jsonString = JsonSerializer.Serialize(myModel);

        using var client = new TcpClient("localhost", 1234);
        using var writerStream = client.GetStream();
        using var writer = new StreamWriter(writerStream, Encoding.ASCII);
        writer.WriteLine(jsonString);
        writer.Flush();

        using var readerStream = client.GetStream();
        using var reader = new StreamReader(writerStream, Encoding.ASCII);
        string response = reader.ReadLine();
        return response;
    }

    [SupportedOSPlatform("linux")]
    public static string TransformToXmlUnixSocket(MyModel myModel)
    {
        return TransformToXmlWinPipe(myModel);
    }

    /// <summary>
    /// This method behaves like a named pipe on the windows platform. On Linux, it behaves like a unix domain socket.
    /// </summary>
    /// <param name="myModel"></param>
    /// <returns></returns>
    public static string TransformToXmlWinPipe(MyModel myModel)
    {
        var jsonString = JsonSerializer.Serialize(myModel);

        using var client = new NamedPipeClientStream("transformToXml");
        client.Connect();

        var writer = new StreamWriter(client);
        writer.WriteLine(jsonString);
        writer.Flush();

        using var reader = new StreamReader(client);
        var response = reader.ReadToEnd();

        return response;
    }

    [SupportedOSPlatform("linux")]
    public static string TransformToXmlUnixPipe(MyModel myModel)
    {
        const string UnixNamedPipePath = "/tmp/transformToXml";

        var jsonString = JsonSerializer.Serialize(myModel);
        using (var writer = new StreamWriter(UnixNamedPipePath))
        {
            writer.WriteLine(jsonString);
        }

        using var reader = new StreamReader(UnixNamedPipePath);
        string response = reader.ReadToEnd();
        return response;
    }

    static void Main()
    {
        var myModel = new MyModel
        {
            MyProperty = "What an interesting job!"
        };

        var xmlOutput = TransformToXmlUnixSocket(myModel);

        var ipcWrapper = JsonSerializer.Deserialize<IpcWrapper>(xmlOutput);
        if (ipcWrapper.HasError)
        {
            throw new Exception(ipcWrapper.Error);
        }

        Console.WriteLine(ipcWrapper.Data);
    }
}