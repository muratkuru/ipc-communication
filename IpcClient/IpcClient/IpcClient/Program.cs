using System.Net.Sockets;
using System.Text;
using System.Text.Json;

class MyModel
{
    public string myProperty { get; set; }
}

internal class Program
{
    public static string TransformToXml(MyModel myModel)
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

    static void Main()
    {
        var myModel = new MyModel
        {
            myProperty = "What an interesting job!"
        };
        var xmlOutput = TransformToXml(myModel);
        Console.WriteLine(xmlOutput);
    }
}