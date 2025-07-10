package kz.hackload.ticketing.service.provider.domain.places;

public record Row(int number)
{
    @Override
    public String toString()
    {
        return String.valueOf(number);
    }
}
